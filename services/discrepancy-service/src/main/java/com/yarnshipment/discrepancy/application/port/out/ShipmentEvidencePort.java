package com.yarnshipment.discrepancy.application.port.out;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Sevkiyata yüklenen kanıt türleri ({@code EvidenceUploaded} olaylarından); aynı kanıt ikinci kez yazılmaz. */
public interface ShipmentEvidencePort {

    void recordUploaded(UUID evidenceId, UUID shipmentId, String evidenceType, Instant uploadedAt);

    Set<String> findEvidenceTypes(UUID shipmentId);
}
