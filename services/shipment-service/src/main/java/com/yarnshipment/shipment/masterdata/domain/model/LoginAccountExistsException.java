package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Kayıt için zaten hesap var ya da giriş adı alınmış (409, {@code LOGIN_ACCOUNT_EXISTS}). */
public final class LoginAccountExistsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LoginAccountExistsException(String reason) {
        super("Login account already exists reason=" + reason);
    }
}
