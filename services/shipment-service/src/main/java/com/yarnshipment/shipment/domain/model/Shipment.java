package com.yarnshipment.shipment.domain.model;

import com.yarnshipment.shipment.domain.model.Weighing.TareBasis;
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
 * @param parties fasoncu, teslim alan, araç
 * @param planned beklenen yük
 * @param dispatchWeighing geçerli çıkış tartımı; henüz girilmediyse {@code null}
 * @param waybillNo tamamlarken girilen irsaliye numarası
 * @param createdAt oluşturma zamanı
 */
public record Shipment(
        UUID id,
        String shipmentNo,
        UUID siteId,
        ShipmentStatus status,
        ShipmentParties parties,
        PlannedLoad planned,
        @Nullable Weighing dispatchWeighing,
        @Nullable String waybillNo,
        Instant createdAt) {

    public Shipment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentNo, "shipmentNo");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(parties, "parties");
        Objects.requireNonNull(planned, "planned");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID subcontractorId() {
        return parties.subcontractorId();
    }

    /** Çuvallar yalnızca çıkış tartımından önce (DRAFT) atanabilir. */
    public void requireBagsAssignable() {
        if (status != ShipmentStatus.DRAFT) {
            throw new InvalidShipmentStatusException(id, status, ShipmentStatus.AWAITING_DELIVERY);
        }
    }

    /** Depo çıkış tartımını kaydetti (DRAFT → AWAITING_DELIVERY). */
    public DispatchRecording recordDispatch(DispatchMeasurement measurement, Instant recordedAt) {
        return recordDispatch(measurement, recordedAt, null);
    }

    /** Aynı kayıt; çıkış tartımına kayıt anındaki standart dara eklenir. */
    public DispatchRecording recordDispatch(
            DispatchMeasurement measurement, Instant recordedAt, @Nullable TareBasis tareBasis) {
        if (status != ShipmentStatus.DRAFT) {
            throw new InvalidShipmentStatusException(id, status, ShipmentStatus.AWAITING_DELIVERY);
        }
        Weighing weighing = new Weighing(
                measurement.grossKg(), measurement.tareKg(), measurement.bagCount(), recordedAt, tareBasis);
        return new DispatchRecording(
                id, siteId, parties.subcontractorId(), weighing, measurement.scaleTicketNo(), recordedAt);
    }
}
