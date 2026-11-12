package com.yarnshipment.moduleapi.evidence;

import java.util.Optional;
import java.util.UUID;

/** evidence modülünün kanıt sorgusu; isteyenin göremediği kanıt yok sayılır (SEC-01). */
public interface EvidenceQueryApi {

    /**
     * Kanıt özeti.
     *
     * @param id kanıt
     * @param shipmentId bağlı olduğu sevkiyat
     * @param type kanıt türü, ör. {@code WAYBILL}
     * @param status kanıt durumu, {@code VALID} ya da {@code INVALIDATED}
     */
    record EvidenceSummary(UUID id, UUID shipmentId, String type, String status) {}

    /**
     * İsteyenin görebildiği kanıtı döndürür; yok ya da görünmüyorsa boş döner.
     *
     * @throws com.yarnshipment.moduleapi.ModuleUnavailableException modül yanıt vermezse
     */
    Optional<EvidenceSummary> findVisibleEvidence(UUID evidenceId);
}
