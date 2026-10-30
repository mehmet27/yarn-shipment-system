package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort;
import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort.EvidenceRef;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * İtiraza bağlanan kanıtın aynı sevkiyata ait ve beklenen türde olduğunu doğrular. Kanıt evidence-service'te
 * isteyenin token'ıyla okunur; isteyenin göremediği kanıt bağlanamaz.
 */
final class ClaimEvidence {

    static final String QUALITY_CLAIM = "QUALITY_CLAIM";
    static final String LAB_RESULT = "LAB_RESULT";

    private final EvidenceLookupPort evidence;

    ClaimEvidence(EvidenceLookupPort evidence) {
        this.evidence = evidence;
    }

    void requireAll(UUID shipmentId, Collection<UUID> evidenceIds, String evidenceType) {
        for (UUID evidenceId : evidenceIds) {
            Optional<EvidenceRef> found = evidence.findEvidence(evidenceId);
            if (!found.map(ref -> matches(ref, shipmentId, evidenceType)).orElse(false)) {
                throw new ClaimRejectedException(ClaimRejection.EVIDENCE_INVALID, "evidenceId=" + evidenceId);
            }
        }
    }

    private static boolean matches(EvidenceRef ref, UUID shipmentId, String evidenceType) {
        return ref.shipmentId().equals(shipmentId) && ref.evidenceType().equals(evidenceType);
    }
}
