package com.yarnshipment.discrepancy.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** İtiraza bağlanacak kanıtı evidence-service'e sorar; isteyenin göremediği kanıt yok sayılır (SEC-01). */
public interface EvidenceLookupPort {

    /**
     * Kanıt özeti.
     *
     * @param evidenceId kanıt
     * @param shipmentId bağlı olduğu sevkiyat
     * @param evidenceType kanıt türü, ör. {@code QUALITY_CLAIM}
     */
    record EvidenceRef(UUID evidenceId, UUID shipmentId, String evidenceType) {}

    /**
     * Kanıt yoksa ya da isteyene görünmüyorsa boş döner; servis yanıt vermiyorsa
     * {@code EvidenceServiceUnavailableException} fırlatır.
     */
    Optional<EvidenceRef> findEvidence(UUID evidenceId);
}
