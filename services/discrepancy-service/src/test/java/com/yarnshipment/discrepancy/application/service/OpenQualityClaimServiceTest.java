package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.discrepancy.TestClaims;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.claim.ClaimFile;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimCommand;
import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort.EvidenceRef;
import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenQualityClaimServiceTest {

    private static final UUID SHIPMENT = TestDiscrepancies.SHIPMENT;

    private final ClaimPorts ports = new ClaimPorts();
    private final OpenQualityClaimService open = new OpenQualityClaimService(
            ports.claims, ports.discrepancies, ports.inspections, ports.evidence, ports.scopeGuard, ClaimPorts.CLOCK);

    @Test
    @DisplayName("Kalite tamamlanmış sevkiyata fotoğraflı itiraz açar; uyuşmazlık vadesi açılıştan sonradır")
    void shouldOpenClaim() {
        Actor quality = quality();
        when(ports.claims.saveClaim(any())).thenReturn(TestClaims.claim());

        ClaimFile file = open.openQualityClaim(command(quality, "120.5", TestClaims.PHOTO));

        ArgumentCaptor<NewQualityClaim> saved = ArgumentCaptor.forClass(NewQualityClaim.class);
        verify(ports.claims).saveClaim(saved.capture());
        assertThat(saved.getValue().openedBy()).isEqualTo(quality.userId());
        assertThat(saved.getValue().dueAt()).isAfter(TestClaims.WITHIN_WINDOW);
        assertThat(saved.getValue().request().claimedKg()).isEqualByComparingTo("120.5");
        assertThat(file.claim().claimedKg()).isEqualByComparingTo("120.5");
    }

    @Test
    @DisplayName("Başka lokasyonun sevkiyatına itiraz açılamaz")
    void shouldRejectOtherSite() {
        Actor other = TestTokens.actor(TestTokens.token("quality")
                .claim("site_ids", List.of(UUID.randomUUID().toString()))
                .build());

        assertThatThrownBy(() -> open.openQualityClaim(command(other, "120.5", TestClaims.PHOTO)))
                .isInstanceOf(OutOfScopeAccessException.class);
        verify(ports.claims, never()).saveClaim(any());
    }

    @Test
    @DisplayName("Kilosu aşan, başka sevkiyatın ya da başka türde kanıt ve tamamlanmamış sevkiyat reddedilir")
    void shouldRejectInvalidClaims() {
        UUID foreign = UUID.randomUUID();
        when(ports.evidence.findEvidence(foreign))
                .thenReturn(Optional.of(new EvidenceRef(foreign, UUID.randomUUID(), ClaimEvidence.QUALITY_CLAIM)));

        assertThat(rejectionOf("2000.001", TestClaims.PHOTO)).isEqualTo(ClaimRejection.CLAIM_EXCEEDS_SHIPMENT_WEIGHT);
        assertThat(rejectionOf("10", foreign)).isEqualTo(ClaimRejection.EVIDENCE_INVALID);
        assertThat(rejectionOf("10", TestClaims.LAB_DOCUMENT)).isEqualTo(ClaimRejection.EVIDENCE_INVALID);
        when(ports.claims.findTarget(SHIPMENT)).thenReturn(Optional.empty());
        assertThat(rejectionOf("10", TestClaims.PHOTO)).isEqualTo(ClaimRejection.NOT_AVAILABLE);
        verify(ports.claims, never()).saveClaim(any());
    }

    private ClaimRejection rejectionOf(String kg, UUID photo) {
        return TestClaims.rejectionOf(() -> open.openQualityClaim(command(quality(), kg, photo)));
    }

    private static OpenQualityClaimCommand command(Actor actor, String kg, UUID photo) {
        return new OpenQualityClaimCommand(
                actor, SHIPMENT, new ClaimRequest("B kalite çıktı", new BigDecimal(kg), null, List.of(photo)));
    }

    private static Actor quality() {
        return TestTokens.actor("quality");
    }
}
