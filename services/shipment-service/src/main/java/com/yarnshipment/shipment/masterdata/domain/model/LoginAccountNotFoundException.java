package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** İstenen giriş hesabı kimlik sağlayıcıda yok (404, {@code LOGIN_ACCOUNT_NOT_FOUND}). */
public final class LoginAccountNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LoginAccountNotFoundException() {
        super("Login account not found");
    }
}
