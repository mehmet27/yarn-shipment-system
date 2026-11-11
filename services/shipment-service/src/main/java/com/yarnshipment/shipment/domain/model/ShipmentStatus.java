package com.yarnshipment.shipment.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Sevkiyat durumu (saha analizi, 10.11.2026): müdür açar, depo çıkış yapınca teslim bekler, irsaliye müdüre
 * ulaşınca tamamlanır. Geçişler veritabanındaki {@code guard_shipment_update} tetikleyicisiyle aynıdır.
 */
public enum ShipmentStatus {
    DRAFT,
    /** Kantar uyuşmazlığı: tartı beklenen yükü tutmadı, müdür kararı bekleniyor. */
    WEIGHT_MISMATCH,
    AWAITING_DELIVERY,
    COMPLETED,
    CANCELLED;

    /** İzin verilen sonraki durumlar. */
    public Set<ShipmentStatus> nextStatuses() {
        return switch (this) {
            case DRAFT -> EnumSet.of(AWAITING_DELIVERY, WEIGHT_MISMATCH, CANCELLED);
            case WEIGHT_MISMATCH -> EnumSet.of(DRAFT, AWAITING_DELIVERY, CANCELLED);
            case AWAITING_DELIVERY -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(ShipmentStatus.class);
        };
    }

    public boolean canTransitionTo(ShipmentStatus target) {
        return nextStatuses().contains(target);
    }
}
