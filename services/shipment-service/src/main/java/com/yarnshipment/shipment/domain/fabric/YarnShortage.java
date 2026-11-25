package com.yarnshipment.shipment.domain.fabric;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Fasoncuda yetmeyen iplik.
 *
 * @param yarnType iplik cinsinin adı
 * @param lotId iplik partisi
 * @param lotNo parti numarası
 * @param requiredKg kumaş için gereken kilo
 * @param missingKg eksik kilo
 */
public record YarnShortage(
        String yarnType, @Nullable UUID lotId, @Nullable String lotNo, BigDecimal requiredKg, BigDecimal missingKg) {}
