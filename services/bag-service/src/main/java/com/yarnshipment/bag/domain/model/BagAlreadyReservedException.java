package com.yarnshipment.bag.domain.model;

import java.io.Serial;

/** Çuval başka bir sevkiyata ayrılmış (409, {@code BAG_ALREADY_RESERVED}). */
public final class BagAlreadyReservedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BagAlreadyReservedException(String uniqueKey) {
        super("Bag already reserved uniqueKey=" + uniqueKey);
    }
}
