package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Sevkiyat yola çıktıktan sonra kantar fişi yenilenemez (409, {@code SHIPMENT_DEPARTED}). */
public final class ShipmentDepartedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ShipmentDepartedException(UUID shipmentId) {
        super("Shipment already departed shipmentId=" + shipmentId);
    }
}
