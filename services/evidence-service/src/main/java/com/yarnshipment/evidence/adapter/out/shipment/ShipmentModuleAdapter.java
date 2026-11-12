package com.yarnshipment.evidence.adapter.out.shipment;

import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import com.yarnshipment.evidence.domain.model.ShipmentServiceUnavailableException;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi.ShipmentSummary;
import com.yarnshipment.moduleapi.shipment.ShipmentNotVisibleException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link ShipmentAccessPort}'u shipment modülünün {@link ShipmentAccessApi} arayüzüne bağlar. */
@Component
final class ShipmentModuleAdapter implements ShipmentAccessPort {

    private static final Set<String> LOCKED_STATUSES = Set.of("COMPLETED", "CANCELLED");
    private static final String COMPLETED = "COMPLETED";

    private final ShipmentAccessApi shipmentApi;

    ShipmentModuleAdapter(ShipmentAccessApi shipmentApi) {
        this.shipmentApi = shipmentApi;
    }

    @Override
    public ShipmentSnapshot findAccessible(UUID shipmentId) {
        try {
            ShipmentSummary shipment = shipmentApi.findVisibleShipment(shipmentId);
            return new ShipmentSnapshot(
                    shipmentId,
                    shipment.siteId(),
                    LOCKED_STATUSES.contains(shipment.status()),
                    COMPLETED.equals(shipment.status()));
        } catch (ShipmentNotVisibleException exception) {
            throw new ShipmentNotAccessibleException(shipmentId);
        } catch (ModuleUnavailableException exception) {
            throw new ShipmentServiceUnavailableException("Shipment module call failed", exception);
        }
    }
}
