package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.adapter.in.web.api.EvidenceApi;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceView;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kanıt okuma ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class EvidenceController implements EvidenceApi {

    private final GetEvidenceUseCase getEvidence;
    private final CurrentActorProvider currentActorProvider;

    public EvidenceController(GetEvidenceUseCase getEvidence, CurrentActorProvider currentActorProvider) {
        this.getEvidence = getEvidence;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('EVIDENCE_VIEW')")
    public ResponseEntity<EvidenceView> getEvidence(UUID evidenceId) {
        return ResponseEntity.ok(EvidenceViews.toView(
                getEvidence.getEvidence(new GetEvidenceQuery(currentActorProvider.getCurrentActor(), evidenceId))));
    }
}
