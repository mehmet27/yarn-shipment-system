package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.Objects;
import java.util.Set;

/**
 * Tek sevkiyatın isteyene göre hazırlanmış hali.
 *
 * @param shipment sevkiyat
 * @param audience web katmanının seçeceği görünüm
 * @param allowedActions isteyenin şu an yapabileceği işlemler
 */
public record ShipmentDetails(Shipment shipment, ShipmentAudience audience, Set<ShipmentAction> allowedActions) {

    public ShipmentDetails {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(audience, "audience");
        allowedActions = Set.copyOf(allowedActions);
    }
}
