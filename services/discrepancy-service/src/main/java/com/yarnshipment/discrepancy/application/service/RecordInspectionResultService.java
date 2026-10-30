package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.claim.RecordInspectionResultCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.RecordInspectionResultUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.InspectionNotFoundException;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Numune ya da lab sonucunu girer; belge varsa itirazın sevkiyatına ait {@code LAB_RESULT} olmalıdır. */
@Service
public class RecordInspectionResultService implements RecordInspectionResultUseCase {

    private static final String ACTION = "INSPECTION_RESULT";

    private final InspectionPort inspections;
    private final ClaimAccess access;
    private final ClaimEvidence evidence;
    private final Clock clock;

    public RecordInspectionResultService(
            QualityClaimPort claims,
            DiscrepancyQueryPort discrepancies,
            InspectionPort inspections,
            EvidenceLookupPort evidence,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.inspections = inspections;
        this.access = new ClaimAccess(claims, discrepancies, inspections, scopeGuard);
        this.evidence = new ClaimEvidence(evidence);
        this.clock = clock;
    }

    @Override
    @Transactional
    public Inspection recordInspectionResult(RecordInspectionResultCommand command) {
        QualityClaim claim = access.loadClaim(command.actor(), command.claimId(), ACTION);
        access.unresolvedDiscrepancyOf(claim);
        Inspection inspection = inspections
                .findInspection(command.inspectionId())
                .filter(found -> found.discrepancyId().equals(claim.discrepancyId()))
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        UUID evidenceId = command.evidenceId();
        Inspection recorded = inspection.recordResult(new Inspection.Result(
                command.summary().strip(), evidenceId, command.actor().userId(), clock.instant()));
        if (evidenceId != null) {
            evidence.requireAll(claim.shipmentId(), List.of(evidenceId), ClaimEvidence.LAB_RESULT);
        }
        inspections.saveResult(recorded);
        return recorded;
    }
}
