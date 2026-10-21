package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Şoförün yükü teslim aldığını beyan etmesi. Şoför çıkışta kaydedilen çuval sayısını ve mühür numarasını onaylar.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param driverId beyan eden şoför
 * @param bagCount onaylanan çuval sayısı
 * @param sealNo onaylanan mühür numarası
 * @param declaredAt sunucunun beyanı aldığı zaman (DOM-11)
 */
public record PickupConfirmation(
        UUID shipmentId,
        UUID siteId,
        UUID driverId,
        int bagCount,
        @Nullable String sealNo,
        Instant declaredAt) {

    public PickupConfirmation {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(driverId, "driverId");
        Objects.requireNonNull(declaredAt, "declaredAt");
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.LOADED;
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.IN_TRANSIT;
    }
}
