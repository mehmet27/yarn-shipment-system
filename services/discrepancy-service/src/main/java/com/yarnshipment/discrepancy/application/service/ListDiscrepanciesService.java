package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kullanıcının lokasyon kapsamındaki uyuşmazlıkları listeler. Fasoncu yalnızca kendi sevkiyatlarınınkini görür
 * (sorgu {@code subcontractor_id} ile süzülür); şoför hiçbir şey görmez.
 */
@Service
public class ListDiscrepanciesService implements ListDiscrepanciesUseCase {

    private final DiscrepancyQueryPort queryPort;
    private final ScopeGuard scopeGuard;

    public ListDiscrepanciesService(DiscrepancyQueryPort queryPort, ScopeGuard scopeGuard) {
        this.queryPort = queryPort;
        this.scopeGuard = scopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscrepancyPage listDiscrepancies(ListDiscrepanciesQuery query) {
        AccessScope scope = scopeGuard.scopeOf(query.actor());
        boolean noSites = !scope.allSites() && scope.siteIds().isEmpty();
        if (scope.driverId() != null || noSites) {
            return DiscrepancyPage.empty(query.page(), query.size());
        }
        return queryPort.findDiscrepancies(query.filter(), scope, query.page(), query.size());
    }
}
