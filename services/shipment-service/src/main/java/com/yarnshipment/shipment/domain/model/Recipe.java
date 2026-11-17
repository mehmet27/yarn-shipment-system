package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** Reçete satırları üzerindeki hesaplar. Kilolar BigDecimal'dir, 3 ondalıktır (KR-04). */
public final class Recipe {

    private static final int KG_SCALE = 3;

    private Recipe() {}

    public static BigDecimal totalKg(List<RecipeLine> lines) {
        return lines.stream().map(RecipeLine::netKg).reduce(BigDecimal.ZERO.setScale(KG_SCALE), BigDecimal::add);
    }

    /** Reçete toplamı tartılan net'ten en çok tolerans kadar farklıysa reçete tartıyı tutuyor demektir. */
    public static boolean fitsWeighing(List<RecipeLine> lines, BigDecimal measuredKg, BigDecimal toleranceKg) {
        return totalKg(lines).subtract(measuredKg).abs().compareTo(toleranceKg) <= 0;
    }
}
