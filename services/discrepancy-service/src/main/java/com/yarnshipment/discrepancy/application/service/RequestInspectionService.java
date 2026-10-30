package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.claim.RequestInspectionCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.RequestInspectionUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort.NewInspection;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.time.Clock;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Karara bağlanmamış itiraz için numune ya da lab kontrolü ister. */
@Service
public class RequestInspectionService implements RequestInspectionUseCase {

    private static final String ACTION = "INSPECTION_REQUEST";

    private final InspectionPort inspections;
    private final ClaimAccess access;
    private final Clock clock;

    public RequestInspectionService(
            QualityClaimPort claims,
            DiscrepancyQueryPort discrepancies,
            InspectionPort inspections,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.inspections = inspections;
        this.access = new ClaimAccess(claims, discrepancies, inspections, scopeGuard);
        this.clock = clock;
    }

    @Override
    @Transactional
    public Inspection requestInspection(RequestInspectionCommand command) {
        QualityClaim claim = access.loadClaim(command.actor(), command.claimId(), ACTION);
        Discrepancy discrepancy = access.unresolvedDiscrepancyOf(claim);
        return inspections.saveRequest(new NewInspection(
                claim.shipmentId(),
                discrepancy.id(),
                command.kind(),
                new Inspection.Request(stripped(command.note()), command.actor().userId(), clock.instant())));
    }

    private static @Nullable String stripped(@Nullable String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }
}
