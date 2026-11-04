package com.yarnshipment.reporting.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Rapor kapsamını çözer (SEC-03): yalnızca iç kullanıcılar rapor görür ve yalnızca yetkili oldukları lokasyonları.
 * Şoför ve fasoncu ile {@code site_ids} claim'i boş kullanıcı boş sonuç alır.
 */
@Component
final class ReportAccess {

    private final ScopeGuard scopeGuard;

    ReportAccess(ScopeGuard scopeGuard) {
        this.scopeGuard = scopeGuard;
    }

    /** Boş dönerse kullanıcı hiçbir kayıt görmez. İstenen lokasyon yetkisizse {@link OutOfScopeAccessException}. */
    Optional<ReportScope> scopeOf(ReportQuery query) {
        Actor actor = query.actor();
        AccessScope scope = scopeGuard.scopeOf(actor);
        boolean external = scope.subcontractorId() != null || scope.driverId() != null;
        boolean noSites = !scope.allSites() && scope.siteIds().isEmpty();
        if (external || noSites) {
            return Optional.empty();
        }
        if (query.siteId() != null && !scope.allSites() && !scope.siteIds().contains(query.siteId())) {
            throw new OutOfScopeAccessException("site:" + query.siteId());
        }
        return Optional.of(new ReportScope(scope.siteIds(), scope.allSites(), query.siteId()));
    }
}
