package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.domain.model.PickupConfirmation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Kaydedilen beyanları bellekte tutar. */
final class RecordingShipmentCommandPort implements ShipmentCommandPort {

    private final List<PickupConfirmation> pickups = new ArrayList<>();

    List<PickupConfirmation> pickups() {
        return pickups;
    }

    void addExistingPickup(PickupConfirmation confirmation) {
        pickups.add(confirmation);
    }

    @Override
    public boolean hasPickupConfirmation(UUID shipmentId) {
        return pickups.stream().anyMatch(pickup -> pickup.shipmentId().equals(shipmentId));
    }

    @Override
    public void recordPickup(PickupConfirmation confirmation, UUID actorUserId) {
        pickups.add(confirmation);
    }
}
