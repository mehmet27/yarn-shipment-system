package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import java.util.Optional;
import java.util.UUID;

/** evidence-service'teki kanıtları isteyenin yetkisiyle sorar. */
public interface EvidenceLookupPort {

    /** Kanıt yoksa ya da isteyene görünmüyorsa boş döner. */
    Optional<EvidenceReference> findEvidence(UUID evidenceId);
}
