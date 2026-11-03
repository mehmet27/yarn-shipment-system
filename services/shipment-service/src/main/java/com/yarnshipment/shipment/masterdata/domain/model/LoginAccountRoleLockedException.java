package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/**
 * Hesabın rolü buradan değiştirilemez: şoför/fasoncu hesapları kayda bağlıdır (409,
 * {@code LOGIN_ACCOUNT_ROLE_LOCKED}).
 */
public final class LoginAccountRoleLockedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LoginAccountRoleLockedException() {
        super("Login account role is bound to a record and cannot be changed");
    }
}
