package com.yarnshipment.shipment.domain.model;

import java.io.Serial;
import java.time.LocalDate;
import java.util.UUID;

/** Sevkiyat günü gelmeden depo işlemi yapılamaz (409, {@code SHIPMENT_NOT_DUE}). */
public final class ShipmentNotDueException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final LocalDate shipmentDate;

    public ShipmentNotDueException(UUID shipmentId, LocalDate shipmentDate) {
        super("Shipment is not due yet shipmentId=" + shipmentId + " shipmentDate=" + shipmentDate);
        this.shipmentDate = shipmentDate;
    }

    public LocalDate shipmentDate() {
        return shipmentDate;
    }
}
