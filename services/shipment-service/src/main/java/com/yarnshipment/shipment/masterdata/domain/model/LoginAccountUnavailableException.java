package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Keycloak hesabı açamadı ya da yapılandırılmamış (502, {@code LOGIN_ACCOUNT_UNAVAILABLE}). */
public final class LoginAccountUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LoginAccountUnavailableException(String message) {
        super(message);
    }

    public LoginAccountUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
