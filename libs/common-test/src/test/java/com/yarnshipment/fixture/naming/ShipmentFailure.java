package com.yarnshipment.fixture.naming;

import java.io.Serial;

/** NAME-15 kasıtlı ihlali: Exception ile bitmeyen istisna. */
public final class ShipmentFailure extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ShipmentFailure(String message) {
        super(message);
    }
}
