package com.yarnshipment.shipment.domain.fire;

import java.io.Serial;
import java.util.UUID;

/** Fire dosyası bu işleme uygun değil (409, {@code INVALID_FIRE_SETTLEMENT_ACTION}); neden kod olarak taşınır. */
public final class InvalidFireSettlementActionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String reason;

    public InvalidFireSettlementActionException(UUID shipmentId, String reason) {
        super("Invalid fire settlement action shipmentId=" + shipmentId + " reason=" + reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
