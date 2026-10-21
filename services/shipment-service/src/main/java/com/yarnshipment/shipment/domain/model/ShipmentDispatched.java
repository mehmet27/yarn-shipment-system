package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Şoför yükü teslim aldı, sevkiyat yolda (olay kataloğu: SevkiyatYolaCikti). Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param driverId şoför
 * @param bagCount onaylanan çuval sayısı
 * @param dispatchedAt beyan zamanı
 */
public record ShipmentDispatched(UUID shipmentId, UUID siteId, UUID driverId, int bagCount, Instant dispatchedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-dispatched.v1";

    public static ShipmentDispatched of(PickupConfirmation confirmation) {
        return new ShipmentDispatched(
                confirmation.shipmentId(),
                confirmation.siteId(),
                confirmation.driverId(),
                confirmation.bagCount(),
                confirmation.declaredAt());
    }
}
