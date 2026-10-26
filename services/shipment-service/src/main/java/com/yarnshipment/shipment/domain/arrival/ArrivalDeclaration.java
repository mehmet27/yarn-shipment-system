package com.yarnshipment.shipment.domain.arrival;

import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Şoförün fasoncuya vardığını beyan etmesi. Şoför mührün sağlam olup olmadığını bildirir; varış zamanı sunucu
 * saatidir.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param driverId beyan eden şoför
 * @param bagCount yola çıkışta onaylanan çuval sayısı
 * @param sealNo çıkışta takılan mühür numarası
 * @param sealIntact varışta mühür sağlam mı
 * @param declaredAt sunucunun beyanı aldığı zaman (DOM-11)
 */
public record ArrivalDeclaration(
        UUID shipmentId,
        UUID siteId,
        UUID driverId,
        int bagCount,
        @Nullable String sealNo,
        boolean sealIntact,
        Instant declaredAt) {

    public ArrivalDeclaration {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(driverId, "driverId");
        Objects.requireNonNull(declaredAt, "declaredAt");
    }

    /** Yoldaki sevkiyatta şoför varışını beyan eder (IN_TRANSIT → AWAITING_DELIVERY). */
    public static ArrivalDeclaration of(
            Shipment shipment, UUID declaringDriverId, boolean sealIntact, Instant declaredAt) {
        if (shipment.status() != ShipmentStatus.IN_TRANSIT) {
            throw new InvalidShipmentStatusException(
                    shipment.id(), shipment.status(), ShipmentStatus.AWAITING_DELIVERY);
        }
        return new ArrivalDeclaration(
                shipment.id(),
                shipment.siteId(),
                declaringDriverId,
                shipment.loadedBagCount(),
                shipment.dispatch().sealNo(),
                sealIntact,
                declaredAt);
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.IN_TRANSIT;
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.AWAITING_DELIVERY;
    }
}
