package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bir şoförün ya da fasoncunun karşılaştırılan sevkiyat göstergeleri.
 *
 * @param id şoför ya da fasoncu
 * @param shipmentCount karşılaştırması tamamlanan sevkiyat
 * @param discrepantShipmentCount uyuşmazlık çıkan sevkiyat
 * @param totalAbsoluteDiffKg mutlak kilo farkı toplamı
 */
public record PartyStats(UUID id, int shipmentCount, int discrepantShipmentCount, BigDecimal totalAbsoluteDiffKg) {

    /** Uyuşmazlıklı sevkiyat yüzdesi, iki ondalık. */
    public BigDecimal discrepancyRate() {
        return Rates.percent(discrepantShipmentCount, shipmentCount);
    }

    /** Sevkiyat başına ortalama mutlak fark, üç ondalık. */
    public BigDecimal averageAbsoluteDiffKg() {
        return Rates.average(totalAbsoluteDiffKg, shipmentCount);
    }
}
