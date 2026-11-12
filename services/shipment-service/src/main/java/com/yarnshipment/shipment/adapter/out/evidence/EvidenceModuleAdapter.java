package com.yarnshipment.shipment.adapter.out.evidence;

import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
import com.yarnshipment.shipment.application.port.out.EvidenceLookupPort;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import com.yarnshipment.shipment.domain.evidence.EvidenceServiceUnavailableException;
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
    public Optional<EvidenceReference> findEvidence(UUID evidenceId) {
        try {
            return evidenceApi
                    .findVisibleEvidence(evidenceId)
                    .map(e -> new EvidenceReference(e.id(), e.shipmentId(), e.type(), e.status()));
        } catch (ModuleUnavailableException exception) {
            throw new EvidenceServiceUnavailableException("Evidence module call failed", exception);
        }
    }
}
