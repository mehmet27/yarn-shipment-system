package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.AssignDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.in.AssignDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Uyuşmazlığı sorumluya atar ve incelemeye alır. */
@Service
public class AssignDiscrepancyService implements AssignDiscrepancyUseCase {

    private static final String ACTION = "DISCREPANCY_ASSIGN";

    private final DiscrepancyDecisionPort decisionPort;
    private final DiscrepancyAccess access;
    private final Clock clock;

    public AssignDiscrepancyService(
            DiscrepancyQueryPort queryPort, DiscrepancyDecisionPort decisionPort, ScopeGuard scopeGuard, Clock clock) {
        this.decisionPort = decisionPort;
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
        this.clock = clock;
    }

    @Override
    @Transactional
    public Discrepancy assignDiscrepancy(AssignDiscrepancyCommand command) {
        Discrepancy discrepancy = access.load(command.actor(), command.discrepancyId(), ACTION);
        Discrepancy assigned = discrepancy.assignTo(command.assigneeId());
        decisionPort.saveAssignment(assigned, command.actor().userId(), clock.instant());
        return assigned;
    }
}
