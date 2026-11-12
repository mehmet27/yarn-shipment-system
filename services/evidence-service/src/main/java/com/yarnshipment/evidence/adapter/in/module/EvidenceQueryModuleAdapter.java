package com.yarnshipment.evidence.adapter.in.module;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Diğer modüllerin {@link EvidenceQueryApi} çağrısını karşılar. REST ucuyla aynı kuralları uygular: EVIDENCE_VIEW izni,
 * sevkiyat görünürlüğü ve lokasyon kapsamı denetlenir, görünmeyen kanıt yok sayılır (SEC-01).
 */
@Component
final class EvidenceQueryModuleAdapter implements EvidenceQueryApi {

    private final GetEvidenceUseCase getEvidence;
    private final CurrentActorProvider currentActorProvider;

    EvidenceQueryModuleAdapter(GetEvidenceUseCase getEvidence, CurrentActorProvider currentActorProvider) {
        this.getEvidence = getEvidence;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    public Optional<EvidenceSummary> findVisibleEvidence(UUID evidenceId) {
        var actor = currentActorProvider.getCurrentActor();
        if (!actor.hasPermission(Permission.EVIDENCE_VIEW)) {
            return Optional.empty();
        }
        try {
            EvidenceFile file = getEvidence.getEvidence(new GetEvidenceQuery(actor, evidenceId));
            return Optional.of(new EvidenceSummary(
                    file.id(),
                    file.shipmentId(),
                    file.declared().type().name(),
                    file.status().name()));
        } catch (EvidenceNotFoundException | AccessDeniedException exception) {
            return Optional.empty();
        }
    }
}
