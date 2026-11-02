package com.yarnshipment.discrepancy.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyata yüklenen kanıt.
 *
 * @param shipmentId sevkiyat
 * @param evidenceId yüklenen kanıt; kapatılan uyuşmazlığın karar notuna yazılır
 * @param evidenceType kanıt türü, ör. {@code SIGNED_DELIVERY_DOC}
 * @param uploadedAt sunucunun alış zamanı
 */
public record RecordEvidenceUploadCommand(UUID shipmentId, UUID evidenceId, String evidenceType, Instant uploadedAt) {

    public RecordEvidenceUploadCommand {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(evidenceType, "evidenceType");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
    }
}
