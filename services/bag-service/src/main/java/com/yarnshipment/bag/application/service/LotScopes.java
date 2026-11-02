package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.bag.domain.model.LotNotLockedException;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import java.util.UUID;

/** Kilitli partiyi getirir; lokasyonunun isteyenin kapsamında olduğunu denetler (GEN-07). */
final class LotScopes {

    private static final String RESOURCE_TYPE = "Lot";

    private LotScopes() {}

    static LockedLot requireLocked(LotQueryPort lots, UUID lotId) {
        return lots.findLocked(lotId).orElseThrow(() -> new LotNotLockedException(lotId));
    }

    static void validateInScope(ScopeGuard scopeGuard, Actor actor, LockedLot lot, String action) {
        scopeGuard.validateInScope(
                actor, new ScopedResource(RESOURCE_TYPE, lot.lotId(), lot.siteId(), null, null), action);
    }
}
