package com.yarnshipment.events.evidence;

import java.time.Instant;
import java.util.UUID;

/**
 * KanitTekrarKullanimiTespitEdildi: aynı dosya (aynı hash) başka bir sevkiyatta daha önce yüklenmişti. Fotoğraf
 * uyuşmazlığını tetikler.
 *
 * @param evidenceId yeni yüklenen kanıt
 * @param shipmentId yeni kanıtın sevkiyatı
 * @param duplicateOfId daha önce yüklenen kanıt
 * @param originalShipmentId daha önce yüklenen kanıtın sevkiyatı
 * @param detectedAt tespit zamanı
 */
public record EvidenceReuseDetected(
        UUID evidenceId, UUID shipmentId, UUID duplicateOfId, UUID originalShipmentId, Instant detectedAt) {

    public static final String TYPE = "com.yarnshipment.evidence.evidence-reuse-detected.v1";
}
