package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.adapter.in.web.api.TicketReadingsApi;
import com.yarnshipment.evidence.adapter.in.web.api.model.DeclareTicketUnverifiedRequest;
import com.yarnshipment.evidence.adapter.in.web.api.model.TicketReadingView;
import com.yarnshipment.evidence.adapter.in.web.api.model.TicketUnverifiedView;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedCommand;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedUseCase;
import com.yarnshipment.evidence.application.port.in.ReadTicketCommand;
import com.yarnshipment.evidence.application.port.in.ReadTicketUseCase;
import com.yarnshipment.evidence.domain.ticket.UnverifiedReason;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kantar fişi okuma ve "doğrulanamadı" uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class TicketReadingController implements TicketReadingsApi {

    private static final String SCALE_UPLOAD = "hasAuthority('EVIDENCE_UPLOAD_DISPATCH')";

    private final ReadTicketUseCase readTicket;
    private final DeclareTicketUnverifiedUseCase declareUnverified;
    private final CurrentActorProvider currentActorProvider;

    public TicketReadingController(
            ReadTicketUseCase readTicket,
            DeclareTicketUnverifiedUseCase declareUnverified,
            CurrentActorProvider currentActorProvider) {
        this.readTicket = readTicket;
        this.declareUnverified = declareUnverified;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(SCALE_UPLOAD)
    public ResponseEntity<TicketReadingView> readEvidenceTicket(UUID evidenceId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(
                TicketReadingViews.toView(readTicket.readTicket(new ReadTicketCommand(actor, evidenceId))));
    }

    @Override
    @PreAuthorize(SCALE_UPLOAD)
    public ResponseEntity<TicketUnverifiedView> declareTicketUnverified(
            UUID evidenceId, DeclareTicketUnverifiedRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        DeclareTicketUnverifiedCommand command = new DeclareTicketUnverifiedCommand(
                actor, evidenceId, UnverifiedReason.valueOf(request.getReason().getValue()));
        return ResponseEntity.ok(TicketReadingViews.toView(declareUnverified.declareUnverified(command)));
    }
}
