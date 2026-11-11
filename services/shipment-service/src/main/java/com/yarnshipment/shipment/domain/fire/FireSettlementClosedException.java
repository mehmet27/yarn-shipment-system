package com.yarnshipment.shipment.domain.fire;

import java.io.Serial;
import java.util.UUID;

/** Fire dosyası kapanmış (409, {@code FIRE_SETTLEMENT_CLOSED}); kumaş girişi, iplik dönüşü ve oran değişmez. */
public final class FireSettlementClosedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public FireSettlementClosedException(UUID shipmentId) {
        super("Fire settlement error shipmentId=" + shipmentId);
    }
}
