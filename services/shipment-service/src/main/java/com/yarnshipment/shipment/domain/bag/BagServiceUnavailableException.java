package com.yarnshipment.shipment.domain.bag;

import java.io.Serial;

/** Çuval servisine ulaşılamadı ya da beklenmeyen bir yanıt verdi (502, {@code BAG_SERVICE_UNAVAILABLE}). */
public final class BagServiceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BagServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public BagServiceUnavailableException(String message) {
        super(message);
    }
}
