package com.yarnshipment.events.stock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * MerkezUretimiYapildi: merkez fabrikada kumaş üretildi ve tükettiği iplik stoktan düşüldü. Kişisel veri taşımaz
 * (EVT-08).
 *
 * @param productionId üretim kaydı
 * @param fabricType kumaş cinsi
 * @param fabricKg üretilen kumaş net kg
 * @param fireRate fire oranı
 * @param producedAt üretim zamanı
 */
public record CentralProductionRecorded(
        UUID productionId, String fabricType, BigDecimal fabricKg, BigDecimal fireRate, Instant producedAt) {

    public static final String TYPE = "com.yarnshipment.stock.central-production-recorded.v1";
}
