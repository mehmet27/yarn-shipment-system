package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Net kilo farkı için tolerans bantları; fark yüzdesi mutlak değerle karşılaştırılır.
 *
 * @param greenMaxPct bu yüzdeye kadar (dahil) Yeşil
 * @param yellowMaxPct bu yüzdeye kadar (dahil) Sarı, üstü Kırmızı
 */
public record ToleranceBands(BigDecimal greenMaxPct, BigDecimal yellowMaxPct) {

    /** Kural tanımlı değilse ön analizdeki öneri (karar #6): Yeşil ≤ %0,5, Sarı ≤ %1,5. */
    public static final ToleranceBands REPORT_DEFAULT =
            new ToleranceBands(new BigDecimal("0.50"), new BigDecimal("1.50"));

    public ToleranceBands {
        Objects.requireNonNull(greenMaxPct, "greenMaxPct");
        Objects.requireNonNull(yellowMaxPct, "yellowMaxPct");
        if (greenMaxPct.signum() < 0 || yellowMaxPct.compareTo(greenMaxPct) <= 0) {
            throw new IllegalArgumentException("Tolerance bands must satisfy 0 <= green < yellow");
        }
    }

    public Band classify(BigDecimal diffPct) {
        BigDecimal absolute = diffPct.abs();
        if (absolute.compareTo(greenMaxPct) <= 0) {
            return Band.GREEN;
        }
        return absolute.compareTo(yellowMaxPct) <= 0 ? Band.YELLOW : Band.RED;
    }
}
