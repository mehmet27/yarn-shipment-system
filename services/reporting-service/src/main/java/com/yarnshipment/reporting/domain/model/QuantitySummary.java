package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;

/**
 * Karşılaştırması tamamlanan sevkiyatların kilo toplamları.
 *
 * @param dispatchNetKg çıkış net toplamı
 * @param deliveryNetKg teslim net toplamı
 * @param absoluteDiffKg sevkiyat başına mutlak farkların toplamı
 * @param comparedShipments karşılaştırılan sevkiyat sayısı
 */
public record QuantitySummary(
        BigDecimal dispatchNetKg, BigDecimal deliveryNetKg, BigDecimal absoluteDiffKg, int comparedShipments) {}
