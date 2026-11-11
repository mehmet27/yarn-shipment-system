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
 * @param status durum
 * @param dispatchNetKg çıkış net kilo; çıkış tartısı yoksa boş
 * @param deliveryNetKg fasoncuya ulaşan net kilo; tamamlanmadıysa boş
 * @param hadDiscrepancy uyuşmazlık çıktı mı
 * @param createdAt oluşturulma
 * @param completedAt tamamlanma
 */
public record ShipmentRow(
        UUID shipmentId,
        String shipmentNo,
        UUID subcontractorId,
        String status,
        @Nullable BigDecimal dispatchNetKg,
        @Nullable BigDecimal deliveryNetKg,
        boolean hadDiscrepancy,
        Instant createdAt,
        @Nullable Instant completedAt) {}
