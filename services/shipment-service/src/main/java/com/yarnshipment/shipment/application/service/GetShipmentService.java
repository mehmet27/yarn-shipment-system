package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.application.port.in.GetShipmentQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentDetails;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import org.springframework.stereotype.Service;

/** Sevkiyatı yükler, kapsamı doğrular (SEC-03) ve isteyenin görünümünü seçer. */
@Service
public final class GetShipmentService implements GetShipmentUseCase {

    private static final String RESOURCE_TYPE = "Shipment";

    private final ShipmentQueryPort shipmentQueryPort;
    private final ScopeGuard scopeGuard;
    private final ShipmentAccessPolicy accessPolicy;

    public GetShipmentService(
            ShipmentQueryPort shipmentQueryPort, ScopeGuard scopeGuard, ShipmentAccessPolicy accessPolicy) {
        this.shipmentQueryPort = shipmentQueryPort;
        this.scopeGuard = scopeGuard;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public ShipmentDetails getShipment(GetShipmentQuery query) {
        Shipment shipment = shipmentQueryPort
                .findShipment(query.shipmentId())
                .orElseThrow(() -> new ShipmentNotFoundException(query.shipmentId()));
        Actor actor = query.actor();
        scopeGuard.validateInScope(actor, scopedResource(shipment), Permission.SHIPMENT_VIEW.name());
        return new ShipmentDetails(
                shipment, accessPolicy.audienceOf(actor), accessPolicy.allowedActions(shipment, actor));
    }

    private static ScopedResource scopedResource(Shipment shipment) {
        return new ScopedResource(
                RESOURCE_TYPE, shipment.id(), shipment.siteId(), shipment.subcontractorId(), shipment.driverId());
    }
}
