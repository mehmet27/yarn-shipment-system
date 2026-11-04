package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Şoför–fasoncu eşleşmesinin göstergeleri.
 *
 * @param driverId şoför
 * @param subcontractorId fasoncu
 * @param shipmentCount karşılaştırması tamamlanan sevkiyat
 * @param discrepantShipmentCount uyuşmazlık çıkan sevkiyat
 * @param discrepancyCount açılan uyuşmazlık sayısı
 * @param totalAbsoluteDiffKg mutlak kilo farkı toplamı
 */
public record PairStats(
        UUID driverId,
        UUID subcontractorId,
        int shipmentCount,
        int discrepantShipmentCount,
        int discrepancyCount,
        BigDecimal totalAbsoluteDiffKg) {

    /** Uyuşmazlıklı sevkiyat yüzdesi, iki ondalık. */
    public BigDecimal discrepancyRate() {
        return Rates.percent(discrepantShipmentCount, shipmentCount);
    }

    /** Sevkiyat başına ortalama mutlak fark, üç ondalık. */
    public BigDecimal averageAbsoluteDiffKg() {
        return Rates.average(totalAbsoluteDiffKg, shipmentCount);
    }
}
