package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.events.discrepancy.DiscrepancyOpened;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** İtiraz yazımı ve okuması; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    RecordingDomainEventPublisher.class,
    ShipmentWatchAdapter.class,
    DiscrepancyWriter.class,
    QualityClaimAdapter.class,
    QualityClaimWriter.class,
    DiscrepancyQueryAdapter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class QualityClaimAdapterTest {

    @Autowired
    private ShipmentWatchAdapter watch;

    @Autowired
    private QualityClaimAdapter claims;

    @Autowired
    private DiscrepancyQueryAdapter discrepancies;

    @Autowired
    private RecordingDomainEventPublisher events;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void cleanDelivery() {
        events.clear();
        SeededClaims.complete(watch);
    }

    @Test
    @DisplayName("İtiraz hedefi izleme kaydından okunur; tamamlanmamış sevkiyat hedef değildir")
    void shouldReadTarget() {
        ClaimTarget target = claims.findTarget(TestComparisons.SHIPMENT).orElseThrow();

        assertThat(target.siteId()).isEqualTo(TestComparisons.SITE);
        assertThat(target.subcontractorId()).isEqualTo(TestComparisons.SUBCONTRACTOR);
        assertThat(target.completedAt()).isEqualTo(TestComparisons.RECEIVED_AT);
        assertThat(target.netKg()).isEqualByComparingTo(TestComparisons.DISPATCH_KG);
        assertThat(claims.findTarget(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("İtiraz Kırmızı QUALITY uyuşmazlığı, kilosu, fotoğrafları ve açılış olayıyla yazılır")
    void shouldSaveClaim() {
        QualityClaim saved = SeededClaims.open(claims);

        QualityClaim stored = claims.findClaims(TestComparisons.SHIPMENT).getFirst();
        assertThat(stored).isEqualTo(saved);
        assertThat(claims.findClaim(saved.id())).contains(saved);
        assertThat(stored.claimedKg()).isEqualByComparingTo("120.5");
        assertThat(stored.fabricReceiptId()).isNull();
        assertThat(stored.evidenceIds()).containsExactly(SeededClaims.PHOTO);
        Discrepancy discrepancy =
                discrepancies.findDiscrepancy(saved.discrepancyId()).orElseThrow();
        assertThat(discrepancy.basis().type()).isEqualTo(DiscrepancyType.QUALITY);
        assertThat(discrepancy.discrepancyNo()).isEqualTo("UY-SV-3-1");
        assertThat(discrepancy.subcontractorId()).isEqualTo(TestComparisons.SUBCONTRACTOR);
        assertThat(discrepancies.findActivity(saved.discrepancyId()))
                .extracting(activity -> activity.actionType())
                .containsExactly("OPENED", "CLAIM_EVIDENCE");
        assertThat(events.count(DiscrepancyOpened.TYPE)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Hedef tarihten sonra açılan itiraz da yazılır; süre sınırı yoktur")
    void shouldSaveClaimAfterTargetDate() {
        QualityClaim saved = SeededClaims.openAfterTarget(claims);

        assertThat(claims.findClaim(saved.id())).contains(saved);
    }
}
