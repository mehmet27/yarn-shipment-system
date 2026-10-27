package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepancyActivityUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Uyuşmazlık geçmişi. */
@Service
public class ListDiscrepancyActivityService implements ListDiscrepancyActivityUseCase {

    private static final String ACTION = "DISCREPANCY_VIEW";

    private final DiscrepancyQueryPort queryPort;
    private final DiscrepancyAccess access;

    public ListDiscrepancyActivityService(DiscrepancyQueryPort queryPort, ScopeGuard scopeGuard) {
        this.queryPort = queryPort;
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyActivity> listActivity(GetDiscrepancyQuery query) {
        access.load(query.actor(), query.discrepancyId(), ACTION);
        return queryPort.findActivity(query.discrepancyId());
    }
}
