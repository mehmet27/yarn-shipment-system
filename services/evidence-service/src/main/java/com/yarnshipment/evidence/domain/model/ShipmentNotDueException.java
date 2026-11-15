package com.yarnshipment.evidence.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/** Sevkiyat günü gelmeden depo çıkış kanıtı yüklenemez (409, {@code SHIPMENT_NOT_DUE}). */
public final class ShipmentNotDueException extends RuntimeException {

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
