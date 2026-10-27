package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.util.Optional;
import java.util.UUID;

/** Kanıt kayıtları. */
public interface EvidenceStorePort {

    Optional<EvidenceFile> findEvidence(UUID evidenceId);

    /**
     * Kaydı, tekrar kullanım tespitini ve olayları aynı transaction'da yazar. Aynı hash başka sevkiyatta daha önce
     * görüldüyse tekrar kaydı ve {@code EvidenceReuseDetected} olayı da yazılır.
     */
    void saveEvidence(EvidenceFile evidence);
}
