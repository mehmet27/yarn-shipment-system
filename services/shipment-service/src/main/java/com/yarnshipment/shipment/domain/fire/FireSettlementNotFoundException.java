package com.yarnshipment.shipment.domain.fire;

import java.io.Serial;
import java.util.UUID;

/** Sevkiyatın fire dosyası yok (404, {@code FIRE_SETTLEMENT_NOT_FOUND}). */
public final class FireSettlementNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FireSettlementNotFoundException(UUID shipmentId) {
        super("Fire settlement error shipmentId=" + shipmentId);
    }
}
