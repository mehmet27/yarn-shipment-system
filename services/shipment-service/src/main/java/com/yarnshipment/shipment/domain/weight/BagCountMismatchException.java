package com.yarnshipment.shipment.domain.weight;

import java.io.Serial;

/**
 * Çıkış tartısında yazılan çuval sayısı sevkiyata eklenen çuval sayısını tutmuyor (409,
 * {@code BAG_COUNT_MISMATCH}); hiçbir şey yazılmaz. Depo çuvalları ya da sayıyı düzeltmelidir.
 */
public final class BagCountMismatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int enteredCount;
    private final int bagCount;

    public BagCountMismatchException(int enteredCount, int bagCount) {
        super("Bag count does not match enteredCount=" + enteredCount + " bagCount=" + bagCount);
        this.enteredCount = enteredCount;
        this.bagCount = bagCount;
    }

    public int enteredCount() {
        return enteredCount;
    }

    public int bagCount() {
        return bagCount;
    }
}
