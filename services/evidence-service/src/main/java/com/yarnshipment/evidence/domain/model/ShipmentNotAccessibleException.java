package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Sevkiyat yok ya da erişim izni yok. */
public final class ShipmentNotAccessibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ShipmentNotAccessibleException(UUID shipmentId) {
        super("Shipment not accessible shipmentId=" + shipmentId);
    }
}
