package com.yarnshipment.shipment.application.port.in;

import java.io.Serial;
import java.util.UUID;

/** İstenen sevkiyat yok (404, {@code SHIPMENT_NOT_FOUND}). */
public final class ShipmentNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ShipmentNotFoundException(UUID shipmentId) {
        super("Shipment not found shipmentId=" + shipmentId);
    }
}
