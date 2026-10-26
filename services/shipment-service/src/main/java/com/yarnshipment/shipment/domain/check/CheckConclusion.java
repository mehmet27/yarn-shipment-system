package com.yarnshipment.shipment.domain.check;

import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kontrol aşamasının sonu: temiz karşılaştırma sevkiyatı tamamlar, uyuşmazlık incelemeye alır (rapor 7.2).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param toStatus {@code COMPLETED} veya {@code IN_REVIEW}
 * @param concludedAt geçiş zamanı
 */
public record CheckConclusion(UUID shipmentId, UUID siteId, ShipmentStatus toStatus, Instant concludedAt) {

    /** Otomatik karşılaştırmanın kişisi yoktur; geçmişte sistem hesabı olarak görünür. */
    public static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    public CheckConclusion {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(toStatus, "toStatus");
        Objects.requireNonNull(concludedAt, "concludedAt");
    }

    public static CheckConclusion of(Shipment shipment, ComparisonOutcome outcome, Instant concludedAt) {
        ShipmentStatus target =
                outcome == ComparisonOutcome.CLEAN ? ShipmentStatus.COMPLETED : ShipmentStatus.IN_REVIEW;
        if (shipment.status() != ShipmentStatus.CHECKING) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), target);
        }
        return new CheckConclusion(shipment.id(), shipment.siteId(), target, concludedAt);
    }

    public ShipmentStatus fromStatus() {
        return ShipmentStatus.CHECKING;
    }

    public boolean completed() {
        return toStatus == ShipmentStatus.COMPLETED;
    }
}
