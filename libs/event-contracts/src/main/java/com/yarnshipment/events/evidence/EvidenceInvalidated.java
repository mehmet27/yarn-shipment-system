package com.yarnshipment.events.evidence;

import java.time.Instant;
import java.util.UUID;

/**
 * KanitGecersizKilindi: yanlış ya da uygunsuz kanıt geçersiz sayıldı; dosya silinmez. Geçersiz kılanın kimliğini
 * taşımaz (EVT-08), o başlıktadır.
 *
 * @param evidenceId kanıt
 * @param shipmentId sevkiyat
 * @param evidenceType kanıt türü, ör. {@code DISPATCH_SCALE}
 * @param reason geçersiz kılma gerekçesi
 * @param invalidatedAt geçersiz kılma zamanı
 */
public record EvidenceInvalidated(
        UUID evidenceId, UUID shipmentId, String evidenceType, String reason, Instant invalidatedAt) {

    public static final String TYPE = "com.yarnshipment.evidence.evidence-invalidated.v1";
}
