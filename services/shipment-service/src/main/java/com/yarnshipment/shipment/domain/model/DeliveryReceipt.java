package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Gönderilmiş kör teslim tutanağı. Gönderildiği an kilitlenir (veritabanında {@code submitted_at}).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param shipmentNo sevkiyat numarası; irsaliyedeki numarayla karşılaştırılır
 * @param plateNo sevkiyatın araç plakası; kantar fişindeki plakayla karşılaştırılır
 * @param subcontractorId sevkiyatın fasoncusu
 * @param receivedBy tutanağı gönderen kullanıcı
 * @param receivedAt sunucunun tutanağı aldığı zaman (DOM-11)
 * @param weighing teslim tartısı
 * @param measurement fasoncunun girdiği değerler
 * @param dispatch çıkış tarafı; yalnızca discrepancy-service'e giden olayda kullanılır (SEC-06)
 */
public record DeliveryReceipt(
        UUID shipmentId,
        UUID siteId,
        String shipmentNo,
        @Nullable String plateNo,
        UUID subcontractorId,
        UUID receivedBy,
        Instant receivedAt,
        Weighing weighing,
        DeliveryMeasurement measurement,
        ShipmentSide dispatch) {

    public DeliveryReceipt {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(shipmentNo, "shipmentNo");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(receivedBy, "receivedBy");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(weighing, "weighing");
        Objects.requireNonNull(measurement, "measurement");
        Objects.requireNonNull(dispatch, "dispatch");
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.AWAITING_DELIVERY;
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.CHECKING;
    }
}
