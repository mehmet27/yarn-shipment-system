package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotFoundException;
import java.util.UUID;

/**
 * Uyuşmazlığı yükler ve kapsam kontrolünden geçirir. Fasoncu yalnızca kendi sevkiyatının uyuşmazlığını görür
 * (yetki matrisi, {@code DISCREPANCY_VIEW} "kendi"); uyuşmazlık karşılaştırmadan sonra açıldığı için kör giriş
 * bozulmaz. Şoföre bağlı uyuşmazlık yoktur, şoför kapsam dışında kalır.
 */
final class DiscrepancyAccess {

    private static final String RESOURCE_TYPE = "Discrepancy";

    private final DiscrepancyQueryPort queryPort;
    private final ScopeGuard scopeGuard;

    DiscrepancyAccess(DiscrepancyQueryPort queryPort, ScopeGuard scopeGuard) {
        this.queryPort = queryPort;
        this.scopeGuard = scopeGuard;
    }

    Discrepancy load(Actor actor, UUID discrepancyId, String action) {
        Discrepancy discrepancy = queryPort
                .findDiscrepancy(discrepancyId)
                .orElseThrow(() -> new DiscrepancyNotFoundException(discrepancyId));
        scopeGuard.validateInScope(
                actor,
                new ScopedResource(
                        RESOURCE_TYPE, discrepancy.id(), discrepancy.siteId(), discrepancy.subcontractorId(), null),
                action);
        return discrepancy;
    }
}
