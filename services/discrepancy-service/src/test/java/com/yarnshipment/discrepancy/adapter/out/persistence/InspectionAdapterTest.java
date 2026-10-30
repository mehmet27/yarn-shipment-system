package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.discrepancy.application.port.out.InspectionPort.NewInspection;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Numune/lab kontrolü yazımı; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    ComparisonAdapter.class,
    QualityClaimAdapter.class,
    QualityClaimWriter.class,
    InspectionAdapter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class InspectionAdapterTest {

    private static final UUID LAB_DOCUMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0004");
    private static final long ONE_MINUTE = 60;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ComparisonAdapter comparisons;

    @Autowired
    private QualityClaimAdapter claims;

    @Autowired
    private InspectionAdapter inspections;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Kontrol istenir ve sonucu bir kez girilir; ikinci sonuç reddedilir")
    void shouldRequestAndRecordInspection() {
        SeededClaims.deliver(comparisons);
        QualityClaim claim = SeededClaims.open(claims);
        Inspection requested = inspections.saveRequest(new NewInspection(
                claim.shipmentId(),
                claim.discrepancyId(),
                InspectionKind.LAB,
                new Inspection.Request("lab'a gönder", SeededClaims.ACTOR, SeededClaims.OPENED_AT)));
        Inspection recorded = requested.recordResult(new Inspection.Result(
                "B kalite doğrulandı",
                LAB_DOCUMENT,
                SeededClaims.ACTOR,
                SeededClaims.OPENED_AT.plusSeconds(ONE_MINUTE)));

        inspections.saveResult(recorded);

        assertThat(inspections.findInspections(claim.discrepancyId())).containsExactly(recorded);
        assertThat(inspections.findInspection(requested.id())).contains(recorded);
        assertThatThrownBy(() -> inspections.saveResult(recorded)).isInstanceOf(ClaimRejectedException.class);
    }
}
