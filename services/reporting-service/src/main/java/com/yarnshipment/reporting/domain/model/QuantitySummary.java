package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;

/**
 * Tamamlanan sevkiyatların kilo toplamları.
 *
 * @param dispatchNetKg çıkış net toplamı
 * @param deliveryNetKg fasoncuya ulaşan net toplam (irsaliyeyle tamamlananlar)
 */
public record QuantitySummary(BigDecimal dispatchNetKg, BigDecimal deliveryNetKg) {}
