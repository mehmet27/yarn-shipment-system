package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Kantar karşılaştırması: boş ve dolu araç tartısı ile beklenen net.
 *
 * @param emptyKg boş araç tartısı (dara)
 * @param loadedKg dolu araç tartısı (brüt)
 * @param expectedKg reçetenin beklenen net kilosu
 * @param toleranceKg o anki kantar toleransı
 * @param shortfallReason deponun eksik yükleme nedeni
 */
public record WeightCheck(
        BigDecimal emptyKg,
        BigDecimal loadedKg,
        BigDecimal expectedKg,
        BigDecimal toleranceKg,
        @Nullable String shortfallReason) {

    public WeightCheck {
        Objects.requireNonNull(emptyKg, "emptyKg");
        Objects.requireNonNull(loadedKg, "loadedKg");
        Objects.requireNonNull(expectedKg, "expectedKg");
        Objects.requireNonNull(toleranceKg, "toleranceKg");
    }

    /** Tartılan net: dolu - boş. */
    public BigDecimal measuredKg() {
        return loadedKg.subtract(emptyKg);
    }

    /** Tartılan - beklenen; eksik yükte negatif. */
    public BigDecimal diffKg() {
        return measuredKg().subtract(expectedKg);
    }

    public boolean exceedsTolerance() {
        return diffKg().abs().compareTo(toleranceKg) > 0;
    }
}
