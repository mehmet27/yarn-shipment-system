package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** İtiraz yazımı ve okuması; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    ComparisonAdapter.class,
    QualityClaimAdapter.class,
    QualityClaimWriter.class,
    DiscrepancyQueryAdapter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class QualityClaimAdapterTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ComparisonAdapter comparisons;

    @Autowired
    private QualityClaimAdapter claims;

    @Autowired
    private DiscrepancyQueryAdapter discrepancies;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void cleanDelivery() {
        SeededClaims.deliver(comparisons);
    }

    @Test
    @DisplayName("Teslim hedefi karşılaştırma girdisinden okunur; okutulan anahtarlar sırasıyla gelir")
    void shouldReadTarget() {
        ClaimTarget target = claims.findTarget(TestComparisons.SHIPMENT).orElseThrow();

        assertThat(target.siteId()).isEqualTo(TestComparisons.SITE);
        assertThat(target.subcontractorId()).isEqualTo(TestComparisons.SUBCONTRACTOR);
        assertThat(target.deliveredKeys()).containsExactlyElementsOf(TestComparisons.KEYS);
        assertThat(claims.findTarget(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("İtiraz Kırmızı QUALITY uyuşmazlığı, çuvalları, fotoğrafları ve açılış olayıyla yazılır")
    void shouldSaveClaim() {
        QualityClaim saved = SeededClaims.open(claims);

        QualityClaim stored = claims.findClaims(TestComparisons.SHIPMENT).getFirst();
        assertThat(stored).isEqualTo(saved);
        assertThat(claims.findClaim(saved.id())).contains(saved);
        assertThat(stored.bagKeys()).containsExactly("UK-0001");
        assertThat(stored.evidenceIds()).containsExactly(SeededClaims.PHOTO);
        Discrepancy discrepancy =
                discrepancies.findDiscrepancy(saved.discrepancyId()).orElseThrow();
        assertThat(discrepancy.basis().type()).isEqualTo(DiscrepancyType.QUALITY);
        assertThat(discrepancy.discrepancyNo()).isEqualTo("UY-SV-3-1");
        assertThat(discrepancy.subcontractorId()).isEqualTo(TestComparisons.SUBCONTRACTOR);
        assertThat(discrepancies.findActivity(saved.discrepancyId()))
                .extracting(activity -> activity.actionType())
                .containsExactly("OPENED", "CLAIM_EVIDENCE");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE event_type = ?", Long.class, DiscrepancyOpened.TYPE))
                .isEqualTo(1L);
    }
}
