package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın okunan hali. Durum değiştiren davranışlar sonraki adımda bu aggregate'e eklenecek.
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
}
