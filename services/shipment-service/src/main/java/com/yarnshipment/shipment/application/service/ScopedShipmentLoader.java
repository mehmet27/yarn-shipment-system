package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Yazma işlemlerinden önce sevkiyatı yükler ve isteyenin kapsamında olduğunu doğrular (SEC-03). */
@Component
public final class ScopedShipmentLoader {

    private static final String RESOURCE_TYPE = "Shipment";

    private final ShipmentQueryPort shipmentQueryPort;
    private final ScopeGuard scopeGuard;

    public ScopedShipmentLoader(ShipmentQueryPort shipmentQueryPort, ScopeGuard scopeGuard) {
        this.shipmentQueryPort = shipmentQueryPort;
        this.scopeGuard = scopeGuard;
    }

    /** Kayıt yoksa 404, kapsam dışıysa audit'e yazılan 403. */
    public Shipment load(Actor actor, UUID shipmentId, Permission permission) {
        Shipment shipment =
                shipmentQueryPort.findShipment(shipmentId).orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
        ScopedResource resource = new ScopedResource(
                RESOURCE_TYPE, shipment.id(), shipment.siteId(), shipment.subcontractorId(), shipment.driverId());
        scopeGuard.validateInScope(actor, resource, permission.name());
        return shipment;
    }
}
