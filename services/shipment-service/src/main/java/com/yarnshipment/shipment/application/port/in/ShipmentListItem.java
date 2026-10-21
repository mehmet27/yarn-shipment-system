package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.Objects;
import java.util.Set;

/**
 * Liste satırı.
 *
 * @param shipment sevkiyat
 * @param allowedActions isteyenin şu an yapabileceği işlemler
 */
public record ShipmentListItem(Shipment shipment, Set<ShipmentAction> allowedActions) {

    public ShipmentListItem {
        Objects.requireNonNull(shipment, "shipment");
        allowedActions = Set.copyOf(allowedActions);
    }
}
