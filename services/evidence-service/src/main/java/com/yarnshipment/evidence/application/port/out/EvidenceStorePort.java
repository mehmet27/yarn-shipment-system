package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Kanıt kayıtları. */
public interface EvidenceStorePort {

    Optional<EvidenceFile> findEvidence(UUID evidenceId);

    /** Sevkiyatın kanıtları, yükleme sırasıyla. */
    List<EvidenceFile> findByShipment(UUID shipmentId);

    /**
     * Kaydı, tekrar kullanım tespitini ve olayları aynı transaction'da yazar. Aynı hash başka sevkiyatta daha önce
     * görüldüyse tekrar kaydı ve {@code EvidenceReuseDetected} olayı da yazılır.
     */
    void saveEvidence(EvidenceFile evidence);
}
