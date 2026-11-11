package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.scope.ScopedResource;
import java.util.UUID;

/**
 * Sevkiyatın kapsam kaynağı. Burada yalnızca lokasyon kapsamı denetlenir.
 */
final class ShipmentScopes {

    private static final String SHIPMENT = "Shipment";

    private ShipmentScopes() {}

    static ScopedResource of(UUID shipmentId, UUID siteId) {
        return new ScopedResource(SHIPMENT, shipmentId, siteId);
    }
}
