package com.yarnshipment.moduleapi.shipment;

import java.io.Serial;
import java.util.UUID;

/** Sevkiyat yok ya da isteyene görünmüyor. */
public final class ShipmentNotVisibleException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ShipmentNotVisibleException(UUID shipmentId) {
        super("Shipment not visible shipmentId=" + shipmentId);
    }
}
