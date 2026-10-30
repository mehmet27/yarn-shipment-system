package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.masterdata.application.port.out.LotPort;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import java.util.UUID;

/**
 * Partiye erişim kuralı: kullanıcı yalnızca kendi lokasyonlarının partilerine dokunur; ana veri yöneticisi
 * ({@code MASTER_DATA_MANAGE}) tüm lokasyonlardadır. Kapsam dışı erişim 403 döner ve audit'e yazılır.
 */
final class LotAccess {

    private static final String KIND = "ProductionLot";

    private final LotPort lots;
    private final ScopeGuard scopeGuard;

    LotAccess(LotPort lots, ScopeGuard scopeGuard) {
        this.lots = lots;
        this.scopeGuard = scopeGuard;
    }

    ProductionLot load(UUID id) {
        return lots.findLot(id).orElseThrow(() -> new MasterDataNotFoundException(KIND, id));
    }

    ProductionLot loadInScope(Actor actor, UUID id) {
        ProductionLot lot = load(id);
        requireSiteInScope(actor, lot.siteId());
        return lot;
    }

    void requireSiteInScope(Actor actor, UUID siteId) {
        if (actor.hasPermission(Permission.MASTER_DATA_MANAGE)) {
            return;
        }
        scopeGuard.validateInScope(
                actor, new ScopedResource("Site", siteId, siteId, null, null), Permission.LOT_QUALITY_LOCK.name());
    }
}
