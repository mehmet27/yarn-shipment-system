package com.yarnshipment.discrepancy.application.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Aynı dosya başka bir sevkiyatta daha önce yüklenmiş ({@code EvidenceReuseDetected}).
 *
 * @param eventId tetikleyen olayın kimliği; idempotency anahtarı (EVT-05)
 * @param eventType tetikleyen olayın tipi
 * @param evidenceId yeni yüklenen kanıt
 * @param shipmentId yeni kanıtın sevkiyatı
 * @param duplicateOfId aynı dosyanın önceki kaydı
 */
public record OpenReusedEvidenceCommand(
        UUID eventId, String eventType, UUID evidenceId, UUID shipmentId, UUID duplicateOfId) {

    public OpenReusedEvidenceCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(duplicateOfId, "duplicateOfId");
    }
}
