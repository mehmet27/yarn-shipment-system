package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Uyuşmazlığı karara bağlar. */
@Service
public class ResolveDiscrepancyService implements ResolveDiscrepancyUseCase {

    private static final String ACTION = "DISCREPANCY_RESOLVE";

    private final DiscrepancyDecisionPort decisionPort;
    private final InspectionPort inspections;
    private final DiscrepancyAccess access;
    private final Clock clock;

    public ResolveDiscrepancyService(
            DiscrepancyQueryPort queryPort,
            DiscrepancyDecisionPort decisionPort,
            InspectionPort inspections,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.decisionPort = decisionPort;
        this.inspections = inspections;
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
        this.clock = clock;
    }

    @Override
    @Transactional
    public Discrepancy resolveDiscrepancy(ResolveDiscrepancyCommand command) {
        Discrepancy discrepancy = access.load(command.actor(), command.discrepancyId(), ACTION);
        if (inspections.findInspections(discrepancy.id()).stream().anyMatch(Inspection::isPending)) {
            throw new ClaimRejectedException(ClaimRejection.INSPECTION_PENDING, "discrepancyId=" + discrepancy.id());
        }
        Instant at = clock.instant();
        Discrepancy resolved = discrepancy.resolve(new Discrepancy.Decision(
                command.resolution(),
                command.note().strip(),
                command.liableParty(),
                command.actor().userId(),
                at));
        decisionPort.saveResolution(resolved, command.actor().userId(), at);
        return resolved;
    }
}
