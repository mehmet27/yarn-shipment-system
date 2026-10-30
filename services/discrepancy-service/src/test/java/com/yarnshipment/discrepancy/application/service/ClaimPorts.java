package com.yarnshipment.discrepancy.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.TestClaims;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort;
import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort.EvidenceRef;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * İtiraz servis testlerinin sahte portları: teslim edilmiş sevkiyat, üzerinde açık bir itiraz (UK-0001) ve aynı
 * sevkiyata yüklenmiş bir itiraz fotoğrafı ile bir lab belgesi.
 */
final class ClaimPorts {

    static final Clock CLOCK = Clock.fixed(TestClaims.WITHIN_WINDOW, ZoneOffset.UTC);

    final QualityClaimPort claims = mock(QualityClaimPort.class);
    final DiscrepancyQueryPort discrepancies = mock(DiscrepancyQueryPort.class);
    final InspectionPort inspections = mock(InspectionPort.class);
    final EvidenceLookupPort evidence = mock(EvidenceLookupPort.class);
    final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, CLOCK);

    ClaimPorts() {
        when(claims.findTarget(TestDiscrepancies.SHIPMENT)).thenReturn(Optional.of(TestClaims.target()));
        when(claims.findClaim(TestClaims.CLAIM_ID)).thenReturn(Optional.of(TestClaims.claim(List.of("UK-0001"))));
        when(discrepancies.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.open(TestTokens.SITE_ID)));
        when(evidence.findEvidence(TestClaims.PHOTO))
                .thenReturn(Optional.of(
                        new EvidenceRef(TestClaims.PHOTO, TestDiscrepancies.SHIPMENT, ClaimEvidence.QUALITY_CLAIM)));
        when(evidence.findEvidence(TestClaims.LAB_DOCUMENT))
                .thenReturn(Optional.of(new EvidenceRef(
                        TestClaims.LAB_DOCUMENT, TestDiscrepancies.SHIPMENT, ClaimEvidence.LAB_RESULT)));
    }
}
