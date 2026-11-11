package com.yarnshipment.shipment.domain.fire;

import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Bir sevkiyatın fire hesabı (analiz 8.1). Ara hesapta yuvarlama yapılmaz; yalnız sonuç 3 ondalığa, yüzde 2 ondalığa
 * {@code HALF_UP} ile yuvarlanır (8.3).
 *
 * <pre>
 * T = G - D - I          B = T x (1 - f)        E = B - (K - I)
 * H = max(0, E)          fire = (T - (K - I)) / T
 * </pre>
 *
 * @param fireRate f, sevkiyatın fire oranı
 * @param sentKg G, gönderilen net kg
 * @param returnedKg D, dönen iplik
 * @param claimKg I, haklı itiraz kg
 * @param fabricKg K, gelen kumaş
 * @param baseKg T, fire tabanı
 * @param expectedFabricKg B, beklenen kumaş
 * @param shortageKg E, eksik; kumaş beklenenden fazlaysa eksidir
 * @param faultyKg H, faturalanacak hatalı kg
 * @param actualFirePercent gerçekleşen fire yüzdesi, 2 ondalık
 */
public record FireCalculation(
        BigDecimal fireRate,
        BigDecimal sentKg,
        BigDecimal returnedKg,
        BigDecimal claimKg,
        BigDecimal fabricKg,
        BigDecimal baseKg,
        BigDecimal expectedFabricKg,
        BigDecimal shortageKg,
        BigDecimal faultyKg,
        BigDecimal actualFirePercent) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** G, D, I, K ve f'den hesabı kurar. */
    public static FireCalculation of(Inputs inputs) {
        BigDecimal sentKg = inputs.sentKg();
        BigDecimal returnedKg = inputs.returnedKg();
        BigDecimal claimKg = inputs.claimKg();
        BigDecimal fabricKg = inputs.fabricKg();
        BigDecimal fireRate = inputs.fireRate();
        BigDecimal base = sentKg.subtract(returnedKg).subtract(claimKg);
        BigDecimal expected = base.multiply(BigDecimal.ONE.subtract(fireRate));
        BigDecimal netFabric = fabricKg.subtract(claimKg);
        BigDecimal shortage = expected.subtract(netFabric);
        BigDecimal faulty = shortage.signum() > 0 ? shortage : BigDecimal.ZERO;
        BigDecimal percent = base.signum() > 0
                ? base.subtract(netFabric)
                        .divide(base, MathContext.DECIMAL128)
                        .multiply(HUNDRED)
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
        return new FireCalculation(
                fireRate,
                Kilograms.round(sentKg),
                Kilograms.round(returnedKg),
                Kilograms.round(claimKg),
                Kilograms.round(fabricKg),
                Kilograms.round(base),
                Kilograms.round(expected),
                Kilograms.round(shortage),
                Kilograms.round(faulty),
                percent);
    }

    /**
     * Hesabın girdileri.
     *
     * @param sentKg G, gönderilen net kg
     * @param returnedKg D, dönen iplik
     * @param claimKg I, haklı itiraz kg
     * @param fabricKg K, gelen kumaş
     * @param fireRate f, fire oranı
     */
    public record Inputs(
            BigDecimal sentKg, BigDecimal returnedKg, BigDecimal claimKg, BigDecimal fabricKg, BigDecimal fireRate) {}

    /** Faturalanacak hatalı kilo var mı. */
    public boolean hasFaultyKg() {
        return faultyKg.signum() > 0;
    }
}
