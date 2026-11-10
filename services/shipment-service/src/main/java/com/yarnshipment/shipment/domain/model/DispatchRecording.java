package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Çıkış tartımının kaydı; sevkiyat DRAFT'tan AWAITING_DELIVERY'ye geçer.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param subcontractorId ipliğin gittiği fasoncu
 * @param weighing kantar tartımı
 * @param scaleTicketNo kantar fişi numarası
 * @param recordedAt sunucunun kaydı aldığı zaman (DOM-11)
 */
public record DispatchRecording(
        UUID shipmentId,
        UUID siteId,
        UUID subcontractorId,
        Weighing weighing,
        @Nullable String scaleTicketNo,
        Instant recordedAt) {

    public DispatchRecording {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(weighing, "weighing");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.DRAFT;
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.AWAITING_DELIVERY;
    }
}
