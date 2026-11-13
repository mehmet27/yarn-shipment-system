package com.yarnshipment.shipment.domain.fabric;

import java.io.Serial;
import java.util.UUID;

/** Gelen kumaş toplamı fasoncuya gönderilen iplik kilosunu aşıyor (409, {@code FABRIC_EXCEEDS_SENT_YARN}). */
public final class FabricExceedsSentYarnException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FabricExceedsSentYarnException(UUID shipmentId) {
        super("Fabric exceeds sent yarn shipmentId=" + shipmentId);
    }
}
