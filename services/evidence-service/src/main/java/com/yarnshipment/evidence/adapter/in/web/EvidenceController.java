package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.adapter.in.web.api.EvidenceApi;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceDownloadView;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceListView;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceView;
import com.yarnshipment.evidence.application.port.in.GetEvidenceDownloadUseCase;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.application.port.in.ListEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.ListEvidenceUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kanıt okuma uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class EvidenceController implements EvidenceApi {

    private static final String VIEW = "hasAuthority('EVIDENCE_VIEW')";

    private final GetEvidenceUseCase getEvidence;
    private final GetEvidenceDownloadUseCase getDownload;
    private final ListEvidenceUseCase listEvidence;
    private final CurrentActorProvider currentActorProvider;

    public EvidenceController(
            GetEvidenceUseCase getEvidence,
            GetEvidenceDownloadUseCase getDownload,
            ListEvidenceUseCase listEvidence,
            CurrentActorProvider currentActorProvider) {
        this.getEvidence = getEvidence;
        this.getDownload = getDownload;
        this.listEvidence = listEvidence;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(VIEW)
    public ResponseEntity<EvidenceView> getEvidence(UUID evidenceId) {
        return ResponseEntity.ok(EvidenceViews.toView(getEvidence.getEvidence(query(evidenceId))));
    }

    @Override
    @PreAuthorize(VIEW)
    public ResponseEntity<EvidenceDownloadView> getEvidenceDownloadUrl(UUID evidenceId) {
        return ResponseEntity.ok(EvidenceViews.toView(getDownload.getDownloadLink(query(evidenceId))));
    }

    @Override
    @PreAuthorize(VIEW)
    public ResponseEntity<EvidenceListView> listEvidence(UUID shipmentId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(
                EvidenceViews.toView(listEvidence.listEvidence(new ListEvidenceQuery(actor, shipmentId))));
    }

    private GetEvidenceQuery query(UUID evidenceId) {
        return new GetEvidenceQuery(currentActorProvider.getCurrentActor(), evidenceId);
    }
}
