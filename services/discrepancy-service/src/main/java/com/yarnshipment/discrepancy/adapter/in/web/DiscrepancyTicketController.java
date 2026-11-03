package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.discrepancy.adapter.in.web.api.DiscrepancyTicketsApi;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyTicketView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.MarkTicketMisreadRequest;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.ticket.GetDiscrepancyTicketsUseCase;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadCommand;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadUseCase;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Uyuşmazlık ekranının fiş paneli. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class DiscrepancyTicketController implements DiscrepancyTicketsApi {

    private final GetDiscrepancyTicketsUseCase getTickets;
    private final MarkTicketMisreadUseCase markMisread;
    private final CurrentActorProvider currentActorProvider;

    public DiscrepancyTicketController(
            GetDiscrepancyTicketsUseCase getTickets,
            MarkTicketMisreadUseCase markMisread,
            CurrentActorProvider currentActorProvider) {
        this.getTickets = getTickets;
        this.markMisread = markMisread;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_VIEW')")
    public ResponseEntity<List<DiscrepancyTicketView>> listDiscrepancyTickets(UUID discrepancyId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(
                TicketViews.toViews(getTickets.getTickets(new GetDiscrepancyQuery(actor, discrepancyId)), actor));
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_RESOLVE')")
    public ResponseEntity<List<DiscrepancyTicketView>> markTicketMisread(
            UUID discrepancyId, UUID evidenceId, @Nullable MarkTicketMisreadRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        String note = request == null ? null : request.getNote();
        return ResponseEntity.ok(TicketViews.toViews(
                markMisread.markMisread(new MarkTicketMisreadCommand(actor, discrepancyId, evidenceId, note)), actor));
    }
}
