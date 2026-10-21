package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Çıkış tartımı ve mührün kaydı; sevkiyat DRAFT'tan LOADED'a geçer.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param weighing kantar tartımı
 * @param scaleTicketNo kantar fişi numarası
 * @param sealNo mühür numarası
 * @param recordedAt sunucunun kaydı aldığı zaman (DOM-11)
 */
public record DispatchRecording(
        UUID shipmentId,
        UUID siteId,
        Weighing weighing,
        @Nullable String scaleTicketNo,
        String sealNo,
        Instant recordedAt) {

    public DispatchRecording {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(weighing, "weighing");
        Objects.requireNonNull(sealNo, "sealNo");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.DRAFT;
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.LOADED;
    }
}
