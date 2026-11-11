package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kantar uyuşmazlığı: tartı beklenen yükü tutmadı, müdürün kararı bekleniyor. Bildirim servisi müdüre haber verir.
 * Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param subcontractorId ipliğin gideceği fasoncu
 * @param expectedKg beklenen net
 * @param measuredKg tartılan net
 * @param diffKg tartılan - beklenen
 * @param detectedAt kayıt zamanı
 */
public record WeightMismatchDetected(
        UUID shipmentId,
        UUID siteId,
        UUID subcontractorId,
        BigDecimal expectedKg,
        BigDecimal measuredKg,
        BigDecimal diffKg,
        Instant detectedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.weight-mismatch-detected.v1";

    public static WeightMismatchDetected of(DispatchRecording recording, WeightCheck check) {
        return new WeightMismatchDetected(
                recording.shipmentId(),
                recording.siteId(),
                recording.subcontractorId(),
                check.expectedKg(),
                check.measuredKg(),
                check.diffKg(),
                recording.recordedAt());
    }
}
