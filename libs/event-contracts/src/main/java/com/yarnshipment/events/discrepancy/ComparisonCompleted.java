package com.yarnshipment.events.discrepancy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * KarsilastirmaTamamlandi: teslim girişi çıkış kayıtlarıyla karşılaştırıldı. Temizse sevkiyat tamamlanır,
 * uyuşmazlık varsa incelemeye alınır.
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param comparedAt karşılaştırma zamanı
 * @param outcome {@code CLEAN} ya da {@code DISCREPANCY}
 * @param quantityBand net kilo farkının bandı: {@code GREEN}, {@code YELLOW}, {@code RED}
 * @param diffKg çıkış net − teslim net; çıkış tartısı yoksa boş
 * @param diffPct fark yüzdesi; çıkış tartısı yoksa boş
 * @param discrepancyCount açılan uyuşmazlık sayısı
 * @param highestSeverity en yüksek öncelik: {@code YELLOW} ya da {@code RED}; temizse boş
 */
public record ComparisonCompleted(
        UUID shipmentId,
        UUID siteId,
        Instant comparedAt,
        String outcome,
        String quantityBand,
        @Nullable BigDecimal diffKg,
        @Nullable BigDecimal diffPct,
        int discrepancyCount,
        @Nullable String highestSeverity) {

    public static final String TYPE = "com.yarnshipment.discrepancy.comparison-completed.v1";
    public static final String CLEAN = "CLEAN";
    public static final String DISCREPANCY = "DISCREPANCY";

    public boolean clean() {
        return CLEAN.equals(outcome);
    }
}
