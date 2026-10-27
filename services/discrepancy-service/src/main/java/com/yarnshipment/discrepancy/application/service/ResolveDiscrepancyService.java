package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
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
    private final DiscrepancyAccess access;
    private final Clock clock;

    public ResolveDiscrepancyService(
            DiscrepancyQueryPort queryPort, DiscrepancyDecisionPort decisionPort, ScopeGuard scopeGuard, Clock clock) {
        this.decisionPort = decisionPort;
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
        this.clock = clock;
    }

    @Override
    @Transactional
    public Discrepancy resolveDiscrepancy(ResolveDiscrepancyCommand command) {
        Discrepancy discrepancy = access.load(command.actor(), command.discrepancyId(), ACTION);
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
