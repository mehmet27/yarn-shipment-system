package com.yarnshipment.shipment.domain.arrival;

import java.time.Instant;
import java.util.UUID;

/**
 * Şoför fasoncuya vardı, sevkiyat teslim bekliyor (olay kataloğu: SevkiyatVardi). Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param driverId şoför
 * @param sealIntact şoföre göre mühür sağlam mı
 * @param arrivedAt beyan zamanı
 */
public record ShipmentArrived(UUID shipmentId, UUID siteId, UUID driverId, boolean sealIntact, Instant arrivedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-arrived.v1";

    public static ShipmentArrived of(ArrivalDeclaration declaration) {
        return new ShipmentArrived(
                declaration.shipmentId(),
                declaration.siteId(),
                declaration.driverId(),
                declaration.sealIntact(),
                declaration.declaredAt());
    }
}
