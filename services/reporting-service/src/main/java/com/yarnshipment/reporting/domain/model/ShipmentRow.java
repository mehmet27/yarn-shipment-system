package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyat döküm satırı; okuma modelinin bir sevkiyatı.
 *
 * @param shipmentId sevkiyat
 * @param shipmentNo numara
 * @param subcontractorId fasoncu
 * @param driverId şoför; henüz atanmadıysa boş
 * @param status durum
 * @param dispatchNetKg çıkış net kilo
 * @param deliveryNetKg teslim net kilo
 * @param diffKg çıkış net − teslim net; karşılaştırılmadıysa boş
 * @param diffPct fark yüzdesi
 * @param band {@code GREEN}, {@code YELLOW} ya da {@code RED}; karşılaştırılmadıysa boş
 * @param hadDiscrepancy uyuşmazlık çıktı mı
 * @param createdAt oluşturulma
 * @param completedAt tamamlanma
 */
public record ShipmentRow(
        UUID shipmentId,
        String shipmentNo,
        UUID subcontractorId,
        @Nullable UUID driverId,
        String status,
        @Nullable BigDecimal dispatchNetKg,
        @Nullable BigDecimal deliveryNetKg,
        @Nullable BigDecimal diffKg,
        @Nullable BigDecimal diffPct,
        @Nullable String band,
        boolean hadDiscrepancy,
        Instant createdAt,
        @Nullable Instant completedAt) {}
