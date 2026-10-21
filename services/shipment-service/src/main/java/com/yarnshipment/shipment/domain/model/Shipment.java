package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyat aggregate'i. Durum yalnızca bu tipin metotlarıyla değişir (DOM-13).
 *
 * @param id sevkiyat kimliği (UUIDv7)
 * @param shipmentNo insan okunur numara
 * @param siteId çıkış lokasyonu (GEN-07)
 * @param status durum
 * @param parties fasoncu, şoför, araç
 * @param planned beklenen yük
 * @param dispatch çıkış tarafının verisi
 * @param delivery teslim tarafının verisi
 * @param randomCheckSelected rastgele fiziki kontrol için seçildi mi (SEC-08)
 * @param createdAt oluşturma zamanı
 */
public record Shipment(
        UUID id,
        String shipmentNo,
        UUID siteId,
        ShipmentStatus status,
        ShipmentParties parties,
        PlannedLoad planned,
        ShipmentSide dispatch,
        ShipmentSide delivery,
        boolean randomCheckSelected,
        Instant createdAt) {

    public Shipment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentNo, "shipmentNo");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(parties, "parties");
        Objects.requireNonNull(planned, "planned");
        Objects.requireNonNull(dispatch, "dispatch");
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID subcontractorId() {
        return parties.subcontractorId();
    }

    public @Nullable UUID driverId() {
        return parties.driverId();
    }

    /**
     * Şoför yükü teslim aldı (LOADED → IN_TRANSIT). Onaylanan çuval sayısı çıkış tartısından, yoksa beklenen yükten
     * alınır.
     */
    public PickupConfirmation confirmPickup(UUID declaringDriverId, Instant declaredAt) {
        if (status != ShipmentStatus.LOADED) {
            throw new InvalidShipmentStatusException(id, status, ShipmentStatus.IN_TRANSIT);
        }
        return new PickupConfirmation(id, siteId, declaringDriverId, loadedBagCount(), dispatch.sealNo(), declaredAt);
    }

    private int loadedBagCount() {
        Weighing weighing = dispatch.weighing();
        if (weighing != null) {
            return weighing.bagCount();
        }
        Integer plannedBags = planned.bagCount();
        return plannedBags == null ? 0 : plannedBags;
    }

    /** Fasoncu kör teslim girişini gönderdi (AWAITING_DELIVERY → CHECKING). */
    public DeliveryReceipt receiveDelivery(UUID receivedBy, DeliveryMeasurement measurement, Instant receivedAt) {
        if (status != ShipmentStatus.AWAITING_DELIVERY) {
            throw new InvalidShipmentStatusException(id, status, ShipmentStatus.CHECKING);
        }
        Weighing weighing =
                new Weighing(measurement.grossKg(), measurement.tareKg(), measurement.bagCount(), receivedAt);
        return new DeliveryReceipt(id, receivedBy, receivedAt, weighing, measurement, dispatch);
    }
}
