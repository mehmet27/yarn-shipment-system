package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın fire dosyası ve hesapları (analiz 8.1); sevkiyat ekranındaki fire dosyasıyla aynı formül.
 *
 * @param sentKg G, gönderilen net kg
 * @param returnedYarnKg D, dönen iplik
 * @param fabricKg K, gelen kumaş
 * @param claimKg I, haklı kalite itirazı kilosu
 * @param fireRate f, kesir (0,0400)
 */
@SuppressWarnings("checkstyle:RecordComponentNumber")
public record FireFile(
        UUID shipmentId,
        String shipmentNo,
        UUID subcontractorId,
        String subcontractorName,
        BigDecimal sentKg,
        BigDecimal returnedYarnKg,
        BigDecimal fabricKg,
        BigDecimal fireRate,
        String status,
        @Nullable String invoiceNo,
        @Nullable Instant invoicedAt,
        BigDecimal claimKg) {

    public static final int KG_SCALE = 3;
    public static final int RATE_SCALE = 4;

    /** T = G − D − I. */
    public BigDecimal baseKg() {
        return sentKg.subtract(returnedYarnKg).subtract(claimKg);
    }

    /** Haklı itiraz düşüldükten sonra gelen kumaş: K − I. */
    private BigDecimal netFabricKg() {
        return fabricKg.subtract(claimKg);
    }

    /** B = T × (1 − f), 3 ondalık HALF_UP. */
    public BigDecimal expectedFabricKg() {
        BigDecimal expected = baseKg().multiply(BigDecimal.ONE.subtract(fireRate));
        return expected.setScale(KG_SCALE, RoundingMode.HALF_UP);
    }

    /** Gerçekleşen fire = (T − K) / T, kesir, 4 ondalık; T ≤ 0 ise 0. */
    public BigDecimal actualFireRate() {
        return actualFireRate(baseKg(), netFabricKg());
    }

    public static BigDecimal actualFireRate(BigDecimal baseKg, BigDecimal fabricKg) {
        if (baseKg.signum() <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE);
        }
        return baseKg.subtract(fabricKg).divide(baseKg, RATE_SCALE, RoundingMode.HALF_UP);
    }

    /** Fire sınırı aşıldı mı: hatalı kg sıfırdan büyük. */
    public boolean exceedsLimit() {
        return shortageKg().signum() > 0;
    }

    /** Toplam fire kilosu T − K; fire sınırı içinde ya da dışında, kumaşta kaybolan iplik. */
    public BigDecimal fireKg() {
        return baseKg().subtract(netFabricKg()).setScale(KG_SCALE, RoundingMode.HALF_UP);
    }

    /** H = max(0, B − (K − I)), 3 ondalık. Ara yuvarlama yapılmaz. */
    public BigDecimal shortageKg() {
        BigDecimal exact = baseKg().multiply(BigDecimal.ONE.subtract(fireRate)).subtract(netFabricKg());
        return exact.max(BigDecimal.ZERO).setScale(KG_SCALE, RoundingMode.HALF_UP);
    }
}
