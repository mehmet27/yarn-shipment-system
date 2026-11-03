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
    @DisplayName("Fasoncu kendi teslimine fotoğraflı itiraz açar; uyuşmazlık vadesi açılıştan sonradır")
    void shouldOpenClaim() {
        Actor subcontractor = subcontractor();
        when(ports.claims.saveClaim(any())).thenReturn(TestClaims.claim(List.of("UK-0002")));

        ClaimFile file = open.openQualityClaim(command(subcontractor, "UK-0002", TestClaims.PHOTO));

        ArgumentCaptor<NewQualityClaim> saved = ArgumentCaptor.forClass(NewQualityClaim.class);
        verify(ports.claims).saveClaim(saved.capture());
        assertThat(saved.getValue().openedBy()).isEqualTo(subcontractor.userId());
        assertThat(saved.getValue().dueAt()).isAfter(TestClaims.WITHIN_WINDOW);
        assertThat(file.claim().bagKeys()).containsExactly("UK-0002");
    }

    @Test
    @DisplayName("Başka fasoncunun teslimine itiraz açılamaz")
    void shouldRejectOtherSubcontractor() {
        Actor other = TestTokens.actor(TestTokens.token("subcontractor")
                .claim("subcontractor_id", UUID.randomUUID().toString())
                .build());

        assertThatThrownBy(() -> open.openQualityClaim(command(other, "UK-0002", TestClaims.PHOTO)))
                .isInstanceOf(OutOfScopeAccessException.class);
        verify(ports.claims, never()).saveClaim(any());
    }

    @Test
    @DisplayName(
            "İtiraz edilmiş çuval, başka sevkiyatın ya da başka türde kanıt ve karşılaştırılmamış sevkiyat reddedilir")
    void shouldRejectInvalidClaims() {
        when(ports.claims.findClaims(SHIPMENT)).thenReturn(List.of(TestClaims.claim(List.of("UK-0001"))));
        UUID foreign = UUID.randomUUID();
        when(ports.evidence.findEvidence(foreign))
                .thenReturn(Optional.of(new EvidenceRef(foreign, UUID.randomUUID(), ClaimEvidence.QUALITY_CLAIM)));

        assertThat(rejectionOf("UK-0001", TestClaims.PHOTO)).isEqualTo(ClaimRejection.BAG_ALREADY_CLAIMED);
        assertThat(rejectionOf("UK-0002", foreign)).isEqualTo(ClaimRejection.EVIDENCE_INVALID);
        assertThat(rejectionOf("UK-0002", TestClaims.LAB_DOCUMENT)).isEqualTo(ClaimRejection.EVIDENCE_INVALID);
        when(ports.claims.findTarget(SHIPMENT)).thenReturn(Optional.empty());
        assertThat(rejectionOf("UK-0002", TestClaims.PHOTO)).isEqualTo(ClaimRejection.NOT_AVAILABLE);
        verify(ports.claims, never()).saveClaim(any());
    }

    private ClaimRejection rejectionOf(String key, UUID photo) {
        return TestClaims.rejectionOf(() -> open.openQualityClaim(command(subcontractor(), key, photo)));
    }

    private static OpenQualityClaimCommand command(Actor actor, String key, UUID photo) {
        return new OpenQualityClaimCommand(
                actor, SHIPMENT, new ClaimRequest("B kalite çıktı", List.of(key), List.of(photo)));
    }

    private static Actor subcontractor() {
        return TestTokens.actor(TestTokens.subcontractorToken());
    }
}
