package com.yarnshipment.evidence.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Yükleme isteği ve kanıt kaydı; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EvidenceStoreAdapter.class, UploadIntentAdapter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class EvidenceAdaptersTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private EvidenceStoreAdapter evidence;

    @Autowired
    private UploadIntentAdapter intents;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Bekleyen yükleme yazılıp aynen okunur; olmayan kimlik boş döner")
    void shouldRoundTripIntent() {
        UploadIntent intent = newIntent(UUID.randomUUID());

        intents.saveIntent(intent);

        assertThat(intents.findIntent(intent.id())).contains(intent);
        assertThat(intents.findIntent(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Kanıt, EvidenceUploaded olayıyla birlikte yazılır ve aynen okunur")
    void shouldSaveEvidenceWithOutboxEvent() {
        EvidenceFile file = newEvidence(UUID.randomUUID(), TestUploads.SHA);

        evidence.saveEvidence(file);

        assertThat(evidence.findEvidence(file.id())).contains(file);
        assertThat(outboxCount(file.shipmentId(), "com.yarnshipment.evidence.evidence-uploaded.v1"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Aynı hash başka sevkiyatta daha önce yüklenmişse tekrar kaydı ve olayı yazılır; aynı sevkiyatta yazılmaz")
    void shouldDetectReuseAcrossShipments() {
        String sha = TestUploads.SHA.replace('a', 'b');
        EvidenceFile original = newEvidence(UUID.randomUUID(), sha);
        EvidenceFile sameShipment = newEvidence(original.shipmentId(), sha);
        EvidenceFile reused = newEvidence(UUID.randomUUID(), sha);

        evidence.saveEvidence(original);
        evidence.saveEvidence(sameShipment);
        assertThat(duplicateCount(sameShipment.id())).isZero();

        evidence.saveEvidence(reused);

        assertThat(duplicateCount(reused.id())).isEqualTo(1);
        assertThat(outboxCount(reused.shipmentId(), "com.yarnshipment.evidence.evidence-reuse-detected.v1"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Sevkiyatın kanıtları listelenir; başka sevkiyatın kanıtı karışmaz")
    void shouldListEvidenceByShipment() {
        UUID shipment = UUID.randomUUID();
        EvidenceFile first = newEvidence(shipment, TestUploads.SHA);
        EvidenceFile second = newEvidence(shipment, TestUploads.SHA.replace('a', 'c'));
        EvidenceFile other = newEvidence(UUID.randomUUID(), TestUploads.SHA.replace('a', 'd'));
        evidence.saveEvidence(first);
        evidence.saveEvidence(second);
        evidence.saveEvidence(other);

        assertThat(evidence.findByShipment(shipment)).containsExactlyInAnyOrder(first, second);
    }

    private int outboxCount(UUID shipmentId, String type) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                Integer.class,
                shipmentId,
                type);
        return count == null ? 0 : count;
    }

    private int duplicateCount(UUID evidenceId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM evidence_duplicate WHERE evidence_id = ?", Integer.class, evidenceId);
        return count == null ? 0 : count;
    }

    private static UploadIntent newIntent(UUID shipmentId) {
        UUID id = UUID.randomUUID();
        return new UploadIntent(
                id,
                shipmentId,
                TestTokens.SITE_ID,
                TestUploads.declared(com.yarnshipment.evidence.domain.model.EvidenceType.DISPATCH_SCALE),
                UploadIntent.storageKeyOf(TestUploads.NOW, shipmentId, id),
                USER,
                TestUploads.NOW,
                TestUploads.expiry());
    }

    private static EvidenceFile newEvidence(UUID shipmentId, String sha256) {
        UploadIntent intent = newIntent(shipmentId);
        StoredObject stored = new StoredObject(TestUploads.SIZE, sha256, TestUploads.JPEG);
        return EvidenceFile.confirm(intent, stored, TestUploads.NOW);
    }
}
