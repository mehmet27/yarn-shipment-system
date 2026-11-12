package com.yarnshipment.shipment.adapter.in.module;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi;
import com.yarnshipment.moduleapi.shipment.ShipmentNotVisibleException;
import com.yarnshipment.shipment.application.port.in.GetShipmentQuery;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Diğer modüllerin {@link ShipmentAccessApi} çağrısını karşılar. REST ucuyla aynı kuralları uygular: isteyenin
 * SHIPMENT_VIEW izni ve lokasyon kapsamı denetlenir, görünmeyen sevkiyat yok sayılır (SEC-01).
 */
@Component
final class ShipmentAccessModuleAdapter implements ShipmentAccessApi {

    private final GetShipmentUseCase getShipment;
    private final CurrentActorProvider currentActorProvider;

    ShipmentAccessModuleAdapter(GetShipmentUseCase getShipment, CurrentActorProvider currentActorProvider) {
        this.getShipment = getShipment;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public ShipmentSummary findVisibleShipment(UUID shipmentId) {
        var actor = currentActorProvider.getCurrentActor();
        if (!actor.hasPermission(Permission.SHIPMENT_VIEW)) {
            throw new ShipmentNotVisibleException(shipmentId);
        }
        try {
            Shipment shipment = getShipment
                    .getShipment(new GetShipmentQuery(actor, shipmentId))
                    .shipment();
            return new ShipmentSummary(
                    shipment.id(), shipment.siteId(), shipment.status().name());
        } catch (ShipmentNotFoundException | AccessDeniedException exception) {
            throw new ShipmentNotVisibleException(shipmentId);
        }
    }
}
