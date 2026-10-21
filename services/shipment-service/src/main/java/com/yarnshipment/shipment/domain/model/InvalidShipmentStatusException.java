package com.yarnshipment.shipment.domain.model;

import java.io.Serial;
import java.util.UUID;

/** Sevkiyatın güncel durumu istenen geçişe izin vermiyor (409, {@code INVALID_SHIPMENT_STATUS}). */
public final class InvalidShipmentStatusException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidShipmentStatusException(UUID shipmentId, ShipmentStatus current, ShipmentStatus target) {
        super("Invalid shipment status transition shipmentId=" + shipmentId + " from=" + current + " to=" + target);
    }
}
