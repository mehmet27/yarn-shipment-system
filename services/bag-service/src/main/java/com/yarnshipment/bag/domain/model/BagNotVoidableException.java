package com.yarnshipment.bag.domain.model;

import java.io.Serial;

/** Çuval depoda değil (sevkiyata ayrılmış ya da yolda); iptal edilemez (409, {@code BAG_NOT_VOIDABLE}). */
public final class BagNotVoidableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BagNotVoidableException(String uniqueKey, BagStatus status) {
        super("Bag not voidable uniqueKey=" + uniqueKey + " status=" + status);
    }
}
