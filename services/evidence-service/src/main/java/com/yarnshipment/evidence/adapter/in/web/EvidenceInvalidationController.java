package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.adapter.in.web.api.EvidenceCorrectionsApi;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceView;
import com.yarnshipment.evidence.adapter.in.web.api.model.InvalidateEvidenceRequest;
import com.yarnshipment.evidence.application.port.in.InvalidateEvidenceCommand;
import com.yarnshipment.evidence.application.port.in.InvalidateEvidenceUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kanıt düzeltme ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class EvidenceInvalidationController implements EvidenceCorrectionsApi {

    private final InvalidateEvidenceUseCase invalidateEvidence;
    private final CurrentActorProvider currentActorProvider;

    public EvidenceInvalidationController(
            InvalidateEvidenceUseCase invalidateEvidence, CurrentActorProvider currentActorProvider) {
        this.invalidateEvidence = invalidateEvidence;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('EVIDENCE_INVALIDATE')")
    public ResponseEntity<EvidenceView> invalidateEvidence(UUID evidenceId, InvalidateEvidenceRequest request) {
        InvalidateEvidenceCommand command =
                new InvalidateEvidenceCommand(currentActorProvider.getCurrentActor(), evidenceId, request.getReason());
        return ResponseEntity.ok(EvidenceViews.toView(invalidateEvidence.invalidateEvidence(command)));
    }
}
