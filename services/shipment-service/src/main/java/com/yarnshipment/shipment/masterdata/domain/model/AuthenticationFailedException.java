package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Kullanıcı adı ya da şifre yanlış ya da hesap kapalı; hangisi olduğu söylenmez. */
public final class AuthenticationFailedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
