package com.yarnshipment.shipment.domain.completion;

import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * İrsaliye müdüre ulaştı, sevkiyat tamamlandı (AWAITING_DELIVERY → COMPLETED). Tamamlanan sevkiyat kilitlenir.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param waybillNo irsaliye numarası (kırpılmış, boş değil)
 * @param waybillEvidenceId yüklenen irsaliye belgesi
 * @param completedAt sunucunun kaydı aldığı zaman
 */
public record Completion(UUID shipmentId, UUID siteId, String waybillNo, UUID waybillEvidenceId, Instant completedAt) {

    public static final int MAX_WAYBILL_NO_LENGTH = 64;

    public Completion {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(waybillNo, "waybillNo");
        Objects.requireNonNull(waybillEvidenceId, "waybillEvidenceId");
        Objects.requireNonNull(completedAt, "completedAt");
    }

    /** Yalnızca teslim bekleyen sevkiyat tamamlanır; irsaliye numarası ve belgesi zorunludur. */
    public static Completion of(Shipment shipment, String waybillNo, UUID waybillEvidenceId, Instant completedAt) {
        if (shipment.status() != ShipmentStatus.AWAITING_DELIVERY) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.COMPLETED);
        }
        String text = waybillNo.strip();
        if (text.isEmpty() || text.length() > MAX_WAYBILL_NO_LENGTH) {
            throw new InvalidCompletionException("waybillNo");
        }
        return new Completion(shipment.id(), shipment.siteId(), text, waybillEvidenceId, completedAt);
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.AWAITING_DELIVERY;
    }

    public ShipmentStatus toStatus() {
        return ShipmentStatus.COMPLETED;
    }
}
