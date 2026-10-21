package com.yarnshipment.shipment.domain.model;

import java.util.EnumSet;
import java.util.Set;

/** Sevkiyat durumu (rapor 7.2). Geçişler veritabanındaki {@code guard_shipment_update} tetikleyicisiyle aynıdır. */
public enum ShipmentStatus {
    DRAFT,
    LOADED,
    IN_TRANSIT,
    AWAITING_DELIVERY,
    CHECKING,
    IN_REVIEW,
    COMPLETED,
    CANCELLED;

    /** İzin verilen sonraki durumlar. */
    public Set<ShipmentStatus> nextStatuses() {
        return switch (this) {
            case DRAFT -> EnumSet.of(LOADED, CANCELLED);
            case LOADED -> EnumSet.of(IN_TRANSIT, CANCELLED);
            case IN_TRANSIT -> EnumSet.of(AWAITING_DELIVERY);
            case AWAITING_DELIVERY -> EnumSet.of(CHECKING);
            case CHECKING -> EnumSet.of(COMPLETED, IN_REVIEW);
            case IN_REVIEW -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(ShipmentStatus.class);
        };
    }

    public boolean canTransitionTo(ShipmentStatus target) {
        return nextStatuses().contains(target);
    }
}
