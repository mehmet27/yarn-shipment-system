package com.yarnshipment.bag.application.port.in;

import java.io.Serial;

/** İstenen çuval yok (404, {@code BAG_NOT_FOUND}). */
public final class BagNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BagNotFoundException(String uniqueKey) {
        super("Bag not found uniqueKey=" + uniqueKey);
    }
}
