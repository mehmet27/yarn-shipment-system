package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Kilitli sevkiyata kanıt eklenemez. */
public final class ShipmentLockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ShipmentLockedException(UUID shipmentId) {
        super("Shipment is locked shipmentId=" + shipmentId);
    }
}
