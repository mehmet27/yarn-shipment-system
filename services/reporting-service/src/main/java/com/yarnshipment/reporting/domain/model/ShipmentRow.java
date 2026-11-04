package com.yarnshipment.reporting.domain.model;

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
 * @param comparison kilo ve karşılaştırma değerleri
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
        ShipmentComparison comparison,
        boolean hadDiscrepancy,
        Instant createdAt,
        @Nullable Instant completedAt) {}
