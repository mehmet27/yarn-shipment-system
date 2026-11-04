package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Yeni şifre kurala uymuyor (400, {@code WEAK_PASSWORD}). */
public final class WeakPasswordException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public WeakPasswordException() {
        super("Password rejected");
    }

    public WeakPasswordException(Throwable cause) {
        super("Password rejected", cause);
    }
}
