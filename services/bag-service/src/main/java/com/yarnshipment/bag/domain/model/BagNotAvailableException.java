package com.yarnshipment.bag.domain.model;

import java.io.Serial;

/** Çuvalın durumu sevkiyata eklemeye izin vermiyor (409, {@code BAG_NOT_AVAILABLE}). */
public final class BagNotAvailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BagNotAvailableException(String uniqueKey, BagStatus status) {
        super("Bag not available uniqueKey=" + uniqueKey + " status=" + status);
    }
}
