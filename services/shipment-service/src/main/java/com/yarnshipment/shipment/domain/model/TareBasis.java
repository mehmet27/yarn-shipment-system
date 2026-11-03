package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Tartımın kaydedildiği andaki standart dara. Girilen dara beklenenden izin verilenden fazla saparsa uyarı çıkar;
 * uyarı kaydı engellemez.
 *
 * @param expectedKg standart dara × çuval sayısı
 * @param tolerancePct izin verilen en çok sapma, yüzde
 */
public record TareBasis(BigDecimal expectedKg, BigDecimal tolerancePct) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public TareBasis {
        Objects.requireNonNull(expectedKg, "expectedKg");
        Objects.requireNonNull(tolerancePct, "tolerancePct");
        if (expectedKg.signum() < 0 || tolerancePct.signum() < 0) {
            throw new IllegalArgumentException("Tare basis must not be negative");
        }
    }

    /** Ambalaj başı standart dara ve çuval sayısından beklenen dara; standart sıfırsa karşılaştırma yok. */
    public static @Nullable TareBasis of(BigDecimal standardTareKg, BigDecimal tolerancePct, int bagCount) {
        if (standardTareKg.signum() == 0) {
            return null;
        }
        return new TareBasis(standardTareKg.multiply(BigDecimal.valueOf(bagCount)), tolerancePct);
    }

    /** İşaretli sapma yüzdesi; artı, beklenenden ağır dara demektir. İki ondalığa yuvarlanır. */
    public BigDecimal deviationPct(BigDecimal tareKg) {
        if (expectedKg.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return tareKg.subtract(expectedKg).multiply(HUNDRED).divide(expectedKg, 2, RoundingMode.HALF_UP);
    }

    public boolean exceeded(BigDecimal tareKg) {
        return deviationPct(tareKg).abs().compareTo(tolerancePct) > 0;
    }
}
