package com.yarnshipment.shipment.domain.weight;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Sevkiyata eklenen çuvalların toplamı çıkış tartısındaki araç net kilosunu tutmuyor (409,
 * {@code BAG_WEIGHT_MISMATCH}); hiçbir şey yazılmaz. Depo çuvalları kontrol edip düzeltmelidir.
 */
public final class BagWeightMismatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient BigDecimal bagKg;
    private final transient BigDecimal measuredKg;
    private final transient BigDecimal toleranceKg;

    public BagWeightMismatchException(BigDecimal bagKg, BigDecimal measuredKg, BigDecimal toleranceKg) {
        super("Bag total does not match vehicle net bagKg=" + bagKg + " measuredKg=" + measuredKg + " toleranceKg="
                + toleranceKg);
        this.bagKg = bagKg;
        this.measuredKg = measuredKg;
        this.toleranceKg = toleranceKg;
    }

    public BigDecimal bagKg() {
        return bagKg;
    }

    public BigDecimal measuredKg() {
        return measuredKg;
    }

    public BigDecimal toleranceKg() {
        return toleranceKg;
    }
}
