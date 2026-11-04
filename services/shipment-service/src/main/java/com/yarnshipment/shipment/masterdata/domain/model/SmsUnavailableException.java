package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** SMS gönderilemedi ya da SMS sağlayıcısı yapılandırılmamış (502, {@code SMS_UNAVAILABLE}). */
public final class SmsUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SmsUnavailableException() {
        super("SMS could not be sent");
    }

    public SmsUnavailableException(Throwable cause) {
        super("SMS could not be sent", cause);
    }
}
