package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Uyuşmazlık detayı. */
@Service
public class GetDiscrepancyService implements GetDiscrepancyUseCase {

    private static final String ACTION = "DISCREPANCY_VIEW";

    private final DiscrepancyAccess access;

    public GetDiscrepancyService(DiscrepancyQueryPort queryPort, ScopeGuard scopeGuard) {
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
    }

    @Override
    @Transactional(readOnly = true)
    public Discrepancy getDiscrepancy(GetDiscrepancyQuery query) {
        return access.load(query.actor(), query.discrepancyId(), ACTION);
    }
}
