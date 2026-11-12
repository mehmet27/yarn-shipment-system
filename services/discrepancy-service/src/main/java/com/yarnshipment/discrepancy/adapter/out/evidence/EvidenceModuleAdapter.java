package com.yarnshipment.discrepancy.adapter.out.evidence;

import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort;
import com.yarnshipment.discrepancy.domain.claim.EvidenceServiceUnavailableException;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link EvidenceLookupPort}'u evidence modülünün {@link EvidenceQueryApi} arayüzüne bağlar. */
@Component
final class EvidenceModuleAdapter implements EvidenceLookupPort {

    private final EvidenceQueryApi evidenceApi;

    EvidenceModuleAdapter(EvidenceQueryApi evidenceApi) {
        this.evidenceApi = evidenceApi;
    }

    @Override
    public Optional<EvidenceRef> findEvidence(UUID evidenceId) {
        try {
            return evidenceApi
                    .findVisibleEvidence(evidenceId)
                    .map(e -> new EvidenceRef(e.id(), e.shipmentId(), e.type()));
        } catch (ModuleUnavailableException exception) {
            throw new EvidenceServiceUnavailableException("Evidence module call failed", exception);
        }
    }
}
