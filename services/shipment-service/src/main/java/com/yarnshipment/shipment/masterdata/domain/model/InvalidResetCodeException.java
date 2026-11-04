package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Kod yanlış, süresi dolmuş ya da deneme hakkı bitmiş (400, {@code INVALID_RESET_CODE}). */
public final class InvalidResetCodeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidResetCodeException() {
        super("Invalid password reset code");
    }

    public InvalidResetCodeException(Throwable cause) {
        super("Invalid password reset code", cause);
    }
}
