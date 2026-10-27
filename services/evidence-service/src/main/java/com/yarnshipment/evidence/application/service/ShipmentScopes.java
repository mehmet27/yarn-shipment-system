package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.scope.ScopedResource;
import java.util.UUID;

/**
 * Sevkiyatın kapsam kaynağı. Fasoncu ve şoförün sahiplik denetimini shipment-service zaten yapmıştır (görünmeyen
 * sevkiyat 403/404 döner); burada yalnızca lokasyon kapsamı denetlenir, sahiplik aktörün kendi kimliğiyle doldurulur.
 */
final class ShipmentScopes {

    private static final String SHIPMENT = "Shipment";

    private ShipmentScopes() {}

    static ScopedResource of(Actor actor, UUID shipmentId, UUID siteId) {
        UUID subcontractorId = actor.hasRole(Role.SUBCONTRACTOR) ? actor.subcontractorId() : null;
        UUID driverId = actor.hasRole(Role.DRIVER) ? actor.driverId() : null;
        return new ScopedResource(SHIPMENT, shipmentId, siteId, subcontractorId, driverId);
    }
}
