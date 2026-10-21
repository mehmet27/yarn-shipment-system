package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentBags;
import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kör giriş (SEC-05): çıkış çuvalları fasoncuya ve şoföre, teslim okutmaları depoya ve şoföre hiç okunmaz; yani
 * yasaklı veri sorgulanıp sonradan gizlenmez, hiç yüklenmez.
 */
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
        UUID id = query.shipmentId();
        return switch (audience) {
            case INTERNAL -> new ShipmentBags(audience, bagsPort.findDispatchBags(id), bagsPort.findDeliveryScans(id));
            case WAREHOUSE -> new ShipmentBags(audience, bagsPort.findDispatchBags(id), null);
            case SUBCONTRACTOR -> new ShipmentBags(audience, null, bagsPort.findDeliveryScans(id));
            case DRIVER -> new ShipmentBags(audience, null, null);
        };
    }
}
