package com.yarnshipment.shipment.domain.fire;

import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Bir sevkiyatın fire hesabı (analiz 8.1). Ara hesapta yuvarlama yapılmaz; yalnız sonuç 3 ondalığa, yüzde 2 ondalığa
 * {@code HALF_UP} ile yuvarlanır (8.3).
 *
 * Faturası kesilen kilo (F) kumaş gibi hesaba katılır: fire = (T - (K - I) - F) / T. Eksik kilo (E, H) değişmez.
 * Haklı itiraz kilosu (I) bakiyeden hemen düşülmez, faturalanacak kiloya eklenir: faturalanacak kalan = max(0, H - F)
 * + itirazdan kalan. Faturanın itirazı karşılayan kısmı eksik kumaşı karşılamaz, bu yüzden fire yüzdesine girmez.
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
 * @param invoicedKg bu dosyada şimdiye kadar faturalanan iplik kilosu
 * @param remainingKg henüz faturalanmamış kilo: eksik kumaştan kalan + haklı itirazdan kalan
 * @param claimRemainingKg remainingKg'nin haklı itirazdan gelen kısmı
 * @param claimInvoicedKg faturalanan kilonun haklı itirazı karşılayan kısmı
 */
// İstisna: SIZE-09, analizdeki sütunları bire bir taşır; bölmek hesap adlarını (G, D, I, K…) dağıtır (GIT-10)
@SuppressWarnings("checkstyle:RecordComponentNumber")
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
        BigDecimal actualFirePercent,
        BigDecimal invoicedKg,
        BigDecimal remainingKg,
        BigDecimal claimRemainingKg,
        BigDecimal claimInvoicedKg) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** G, D, I, K ve f'den hesabı kurar. */
    public static FireCalculation of(Inputs inputs) {
        BigDecimal sentKg = inputs.sentKg();
        BigDecimal returnedKg = inputs.returnedKg();
        BigDecimal claimKg = inputs.claimKg();
        BigDecimal fabricKg = inputs.fabricKg();
        BigDecimal base = sentKg.subtract(returnedKg).subtract(claimKg);
        BigDecimal expected = base.multiply(BigDecimal.ONE.subtract(inputs.fireRate()));
        BigDecimal netFabric = fabricKg.subtract(claimKg);
        BigDecimal shortage = expected.subtract(netFabric);
        BigDecimal faulty = shortage.signum() > 0 ? shortage : BigDecimal.ZERO;
        BigDecimal shortageInvoiced = inputs.invoicedKg().subtract(inputs.claimInvoicedKg());
        BigDecimal roundedFaulty = Kilograms.round(faulty);
        BigDecimal claimRemaining = claimRemaining(inputs);
        BigDecimal remaining = remaining(roundedFaulty, shortageInvoiced, claimRemaining);
        return new FireCalculation(
                inputs.fireRate(),
                Kilograms.round(sentKg),
                Kilograms.round(returnedKg),
                Kilograms.round(claimKg),
                Kilograms.round(fabricKg),
                Kilograms.round(base),
                Kilograms.round(expected),
                Kilograms.round(shortage),
                roundedFaulty,
                firePercent(base, netFabric.add(shortageInvoiced)),
                Kilograms.round(inputs.invoicedKg()),
                Kilograms.round(remaining),
                claimRemaining,
                Kilograms.round(inputs.claimInvoicedKg()));
    }

    private static BigDecimal claimRemaining(Inputs inputs) {
        return Kilograms.round(
                inputs.pendingClaimKg().subtract(inputs.claimInvoicedKg()).max(BigDecimal.ZERO));
    }

    /** Faturalanacak kalan: eksik kumaştan faturalanmamış kısım + haklı itirazdan faturalanmamış kısım. */
    private static BigDecimal remaining(BigDecimal faultyKg, BigDecimal shortageInvoiced, BigDecimal claimRemaining) {
        return faultyKg.subtract(Kilograms.round(shortageInvoiced))
                .max(BigDecimal.ZERO)
                .add(claimRemaining);
    }

    private static BigDecimal firePercent(BigDecimal base, BigDecimal netFabric) {
        if (base.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return base.subtract(netFabric)
                .divide(base, MathContext.DECIMAL128)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Hesabın girdileri.
     *
     * @param sentKg G, gönderilen net kg
     * @param returnedKg D, dönen iplik
     * @param claimKg I, haklı itiraz kg
     * @param fabricKg K, gelen kumaş
     * @param fireRate f, fire oranı
     * @param invoicedKg şimdiye kadar faturalanan kilo
     * @param pendingClaimKg I'nın faturayla bakiyeden düşülecek kısmı
     * @param claimInvoicedKg faturalanan kilonun haklı itirazı karşılayan kısmı
     */
    public record Inputs(
            BigDecimal sentKg,
            BigDecimal returnedKg,
            BigDecimal claimKg,
            BigDecimal fabricKg,
            BigDecimal fireRate,
            BigDecimal invoicedKg,
            BigDecimal pendingClaimKg,
            BigDecimal claimInvoicedKg) {

        /** Faturayla düşülecek itirazı olmayan dosya. */
        public Inputs(
                BigDecimal sentKg,
                BigDecimal returnedKg,
                BigDecimal claimKg,
                BigDecimal fabricKg,
                BigDecimal fireRate,
                BigDecimal invoicedKg) {
            this(sentKg, returnedKg, claimKg, fabricKg, fireRate, invoicedKg, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        /** Faturalanmış kilosu olmayan dosya. */
        public Inputs(
                BigDecimal sentKg,
                BigDecimal returnedKg,
                BigDecimal claimKg,
                BigDecimal fabricKg,
                BigDecimal fireRate) {
            this(sentKg, returnedKg, claimKg, fabricKg, fireRate, BigDecimal.ZERO);
        }
    }

    /** Faturalanan kilonun eksik kumaşı karşılayan kısmı; bu kadar iplik tüketilmiş sayılır. */
    public BigDecimal shortageInvoicedKg() {
        return invoicedKg.subtract(claimInvoicedKg);
    }

    /** Faturalanacak (henüz faturalanmamış) hatalı kilo var mı. */
    public boolean hasFaultyKg() {
        return remainingKg.signum() > 0;
    }
}
