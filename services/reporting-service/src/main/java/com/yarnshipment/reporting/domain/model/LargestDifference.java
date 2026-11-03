package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kilo farkı en büyük sevkiyatlardan biri.
 *
 * @param shipmentId sevkiyat
 * @param shipmentNo numara
 * @param diffKg çıkış net − teslim net
 * @param diffPct fark yüzdesi
 * @param band {@code GREEN}, {@code YELLOW} ya da {@code RED}
 */
public record LargestDifference(
        UUID shipmentId,
        String shipmentNo,
        BigDecimal diffKg,
        @Nullable BigDecimal diffPct,
        String band) {}
