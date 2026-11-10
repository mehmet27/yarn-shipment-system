package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentBags;
import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyata atanmış çuvalları kapsam kontrolünden sonra döner. */
@Service
public class GetShipmentBagsService implements GetShipmentBagsUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentAccessPolicy accessPolicy;
    private final ShipmentBagsPort bagsPort;

    public GetShipmentBagsService(
            ScopedShipmentLoader shipments, ShipmentAccessPolicy accessPolicy, ShipmentBagsPort bagsPort) {
        this.shipments = shipments;
        this.accessPolicy = accessPolicy;
        this.bagsPort = bagsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentBags getBags(GetShipmentBagsQuery query) {
        Actor actor = query.actor();
        shipments.load(actor, query.shipmentId(), Permission.SHIPMENT_VIEW);
        ShipmentAudience audience = accessPolicy.audienceOf(actor);
        return new ShipmentBags(audience, bagsPort.findDispatchBags(query.shipmentId()));
    }
}
