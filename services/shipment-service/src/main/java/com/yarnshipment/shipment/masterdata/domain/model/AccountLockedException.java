package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;
import java.time.Instant;

/** Art arda yanlış denemeden sonra hesap kısa süre kilitlidir. */
public final class AccountLockedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked");
        this.lockedUntil = lockedUntil;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
