package com.yarnshipment.evidence.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.evidence.TestTickets;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import com.yarnshipment.evidence.domain.ticket.TicketUnverified;
import com.yarnshipment.evidence.domain.ticket.UnverifiedReason;
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

/** Fiş "doğrulanamadı" gerekçesi; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    EvidenceStoreAdapter.class,
    EvidenceOutbox.class,
    TicketReadingAdapter.class,
    TicketUnverifiedAdapter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class TicketUnverifiedAdapterTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a2");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private EvidenceStoreAdapter evidence;

    @Autowired
    private TicketReadingAdapter readings;

    @Autowired
    private TicketUnverifiedAdapter unverifiedAdapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Gerekçe olayıyla yazılır; ikinci gerekçe ilkini ve olayı değiştirmez")
    void shouldSaveUnverifiedOnce() {
        TicketReading unreadable = TicketReading.evaluate(
                savedEvidence(),
                TicketScan.unreadableImage(ReadingProblem.BLURRY),
                TestTickets.VERSION,
                TestUploads.NOW);
        readings.saveReading(unreadable);
        TicketUnverified first = TicketUnverified.declare(unreadable, UnverifiedReason.TICKET_DAMAGED, TestUploads.NOW);

        unverifiedAdapter.saveUnverified(first);
        unverifiedAdapter.saveUnverified(TicketUnverified.declare(unreadable, UnverifiedReason.OTHER, TestUploads.NOW));

        assertThat(unverifiedAdapter.findUnverified(unreadable.evidenceId())).contains(first);
        assertThat(unverifiedAdapter.findUnverified(UUID.randomUUID())).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        Integer.class,
                        unreadable.shipmentId(),
                        com.yarnshipment.events.evidence.TicketUnverified.TYPE))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT payload ->> 'reason' FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        String.class,
                        unreadable.shipmentId(),
                        com.yarnshipment.events.evidence.TicketUnverified.TYPE))
                .isEqualTo("TICKET_DAMAGED");
    }

    private EvidenceFile savedEvidence() {
        UUID id = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UploadIntent intent = new UploadIntent(
                id,
                shipmentId,
                TestTokens.SITE_ID,
                TestUploads.declared(EvidenceType.DELIVERY_SCALE),
                UploadIntent.storageKeyOf(TestUploads.NOW, shipmentId, id),
                USER,
                TestUploads.NOW,
                TestUploads.expiry());
        EvidenceFile file = EvidenceFile.confirm(intent, TestUploads.stored(), TestUploads.NOW);
        evidence.saveEvidence(file);
        return file;
    }
}
