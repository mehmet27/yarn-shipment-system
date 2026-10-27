package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.adapter.in.web.api.EvidenceUploadsApi;
import com.yarnshipment.evidence.adapter.in.web.api.model.EvidenceView;
import com.yarnshipment.evidence.adapter.in.web.api.model.RequestUploadRequest;
import com.yarnshipment.evidence.adapter.in.web.api.model.UploadTicketView;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadCommand;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadUseCase;
import com.yarnshipment.evidence.application.port.in.RequestUploadCommand;
import com.yarnshipment.evidence.application.port.in.RequestUploadUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Yükleme isteği ve onayı. Method security proxy'si gerektirdiği için final değildir (SEC-02). Aşamaya göre asıl
 * izin denetimi serviste yapılır; burada yalnızca herhangi bir yükleme iznine sahip olma şartı aranır.
 */
@RestController
public class EvidenceUploadController implements EvidenceUploadsApi {

    private static final String ANY_UPLOAD = "hasAnyAuthority('EVIDENCE_UPLOAD_DISPATCH', 'EVIDENCE_UPLOAD_DRIVER',"
            + " 'EVIDENCE_UPLOAD_DELIVERY', 'EVIDENCE_UPLOAD_CLAIM')";

    private final RequestUploadUseCase requestUpload;
    private final ConfirmUploadUseCase confirmUpload;
    private final CurrentActorProvider currentActorProvider;

    public EvidenceUploadController(
            RequestUploadUseCase requestUpload,
            ConfirmUploadUseCase confirmUpload,
            CurrentActorProvider currentActorProvider) {
        this.requestUpload = requestUpload;
        this.confirmUpload = confirmUpload;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(ANY_UPLOAD)
    public ResponseEntity<UploadTicketView> requestEvidenceUpload(UUID idempotencyKey, RequestUploadRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        RequestUploadCommand command =
                new RequestUploadCommand(actor, request.getShipmentId(), EvidenceViews.declaredOf(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EvidenceViews.toView(requestUpload.requestUpload(command)));
    }

    @Override
    @PreAuthorize(ANY_UPLOAD)
    public ResponseEntity<EvidenceView> confirmEvidenceUpload(UUID evidenceId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(
                EvidenceViews.toView(confirmUpload.confirmUpload(new ConfirmUploadCommand(actor, evidenceId))));
    }
}
