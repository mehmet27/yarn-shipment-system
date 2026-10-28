package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.adapter.in.web.api.TicketReadingsApi;
import com.yarnshipment.evidence.adapter.in.web.api.model.TicketReadingView;
import com.yarnshipment.evidence.application.port.in.ReadTicketCommand;
import com.yarnshipment.evidence.application.port.in.ReadTicketUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kantar fişi okuma ucu. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class TicketReadingController implements TicketReadingsApi {

    private static final String SCALE_UPLOAD =
            "hasAnyAuthority('EVIDENCE_UPLOAD_DISPATCH', 'EVIDENCE_UPLOAD_DELIVERY')";

    private final ReadTicketUseCase readTicket;
    private final CurrentActorProvider currentActorProvider;

    public TicketReadingController(ReadTicketUseCase readTicket, CurrentActorProvider currentActorProvider) {
        this.readTicket = readTicket;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(SCALE_UPLOAD)
    public ResponseEntity<TicketReadingView> readEvidenceTicket(UUID evidenceId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(
                TicketReadingViews.toView(readTicket.readTicket(new ReadTicketCommand(actor, evidenceId))));
    }
}
