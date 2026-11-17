package com.yarnshipment.shipment.domain.weight;

import java.io.Serial;
import java.math.BigDecimal;

/** Güncellenen reçetenin toplamı tartılan net'i tutmuyor (409, {@code RECIPE_WEIGHT_MISMATCH}); hiçbir şey yazılmaz. */
public final class RecipeWeightMismatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient BigDecimal recipeKg;
    private final transient BigDecimal measuredKg;
    private final transient BigDecimal toleranceKg;

    public RecipeWeightMismatchException(BigDecimal recipeKg, BigDecimal measuredKg, BigDecimal toleranceKg) {
        super("Recipe total does not match weighing recipeKg=" + recipeKg + " measuredKg=" + measuredKg
                + " toleranceKg=" + toleranceKg);
        this.recipeKg = recipeKg;
        this.measuredKg = measuredKg;
        this.toleranceKg = toleranceKg;
    }

    public BigDecimal recipeKg() {
        return recipeKg;
    }

    public BigDecimal measuredKg() {
        return measuredKg;
    }

    public BigDecimal toleranceKg() {
        return toleranceKg;
    }
}
