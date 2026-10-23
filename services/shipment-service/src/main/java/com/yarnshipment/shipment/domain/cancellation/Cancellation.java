package com.yarnshipment.shipment.domain.cancellation;

import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyatın iptali (DRAFT ya da LOADED → CANCELLED).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param fromStatus iptal edilen sevkiyatın durumu
 * @param reason iptal gerekçesi (kırpılmış, boş değil)
 * @param cancelledAt sunucunun iptali aldığı zaman
 */
public record Cancellation(
        UUID shipmentId, UUID siteId, ShipmentStatus fromStatus, String reason, Instant cancelledAt) {

    public static final int MAX_REASON_LENGTH = 500;

    public Cancellation {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(fromStatus, "fromStatus");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(cancelledAt, "cancelledAt");
    }

    /** Henüz yola çıkmamış sevkiyat iptal edilir (DRAFT ya da LOADED → CANCELLED); gerekçe zorunludur. */
    public static Cancellation of(Shipment shipment, String reason, Instant cancelledAt) {
        ShipmentStatus status = shipment.status();
        if (status != ShipmentStatus.DRAFT && status != ShipmentStatus.LOADED) {
            throw new InvalidShipmentStatusException(shipment.id(), status, ShipmentStatus.CANCELLED);
        }
        String text = reason.strip();
        if (text.isEmpty() || text.length() > MAX_REASON_LENGTH) {
            throw new InvalidCancellationException("reason");
        }
        return new Cancellation(shipment.id(), shipment.siteId(), status, text, cancelledAt);
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.CANCELLED;
    }
}
