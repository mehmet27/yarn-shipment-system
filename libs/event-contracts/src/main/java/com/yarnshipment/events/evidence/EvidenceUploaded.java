package com.yarnshipment.events.evidence;

import java.time.Instant;
import java.util.UUID;

/**
 * KanitYuklendi: dosya nesne deposuna yazıldı, hash'i sunucuda hesaplandı ve kayıt onaylandı. Dosyanın kendisini ve
 * yükleyenin kimliğini taşımaz (EVT-08).
 *
 * @param evidenceId kanıt
 * @param shipmentId sevkiyat
 * @param evidenceType kanıt türü, ör. {@code DISPATCH_SCALE}
 * @param sha256 sunucunun hesapladığı hash
 * @param uploadedAt sunucunun alış zamanı
 */
public record EvidenceUploaded(
        UUID evidenceId, UUID shipmentId, String evidenceType, String sha256, Instant uploadedAt) {

    public static final String TYPE = "com.yarnshipment.evidence.evidence-uploaded.v1";
}
