package com.yarnshipment.shipment.domain.cancellation;

import java.io.Serial;

/** İptal gerekçesi geçersiz (400, {@code INVALID_CANCELLATION}). */
public final class InvalidCancellationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidCancellationException(String reason) {
        super("Invalid cancellation reason=" + reason);
    }
}
