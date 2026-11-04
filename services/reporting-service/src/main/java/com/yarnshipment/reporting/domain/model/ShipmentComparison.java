package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın kilo ve karşılaştırma değerleri; henüz girilmemiş olanlar boştur.
 *
 * @param dispatchNetKg çıkış net kilo
 * @param deliveryNetKg teslim net kilo
 * @param diffKg çıkış net − teslim net
 * @param diffPct fark yüzdesi
 * @param band {@code GREEN}, {@code YELLOW} ya da {@code RED}
 */
public record ShipmentComparison(
        @Nullable BigDecimal dispatchNetKg,
        @Nullable BigDecimal deliveryNetKg,
        @Nullable BigDecimal diffKg,
        @Nullable BigDecimal diffPct,
        @Nullable String band) {}
