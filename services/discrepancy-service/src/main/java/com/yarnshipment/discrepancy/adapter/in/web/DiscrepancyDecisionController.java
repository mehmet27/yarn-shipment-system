package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.discrepancy.adapter.in.web.api.DiscrepancyDecisionsApi;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.AssignDiscrepancyRequest;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ResolveDiscrepancyRequest;
import com.yarnshipment.discrepancy.application.port.in.AssignDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.in.AssignDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyCommand;
import com.yarnshipment.discrepancy.application.port.in.ResolveDiscrepancyUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Atama ve karar uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class DiscrepancyDecisionController implements DiscrepancyDecisionsApi {

    private final AssignDiscrepancyUseCase assignDiscrepancy;
    private final ResolveDiscrepancyUseCase resolveDiscrepancy;
    private final CurrentActorProvider currentActorProvider;

    public DiscrepancyDecisionController(
            AssignDiscrepancyUseCase assignDiscrepancy,
            ResolveDiscrepancyUseCase resolveDiscrepancy,
            CurrentActorProvider currentActorProvider) {
        this.assignDiscrepancy = assignDiscrepancy;
        this.resolveDiscrepancy = resolveDiscrepancy;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_RESOLVE')")
    public ResponseEntity<DiscrepancyView> assignDiscrepancy(UUID discrepancyId, AssignDiscrepancyRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(DiscrepancyViews.toView(
                assignDiscrepancy.assignDiscrepancy(
                        new AssignDiscrepancyCommand(actor, discrepancyId, request.getAssigneeId())),
                actor));
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_RESOLVE')")
    public ResponseEntity<DiscrepancyView> resolveDiscrepancy(UUID discrepancyId, ResolveDiscrepancyRequest request) {
        Actor actor = currentActorProvider.getCurrentActor();
        String liableParty =
                request.getLiableParty() == null || request.getLiableParty().isBlank()
                        ? null
                        : request.getLiableParty().strip();
        ResolveDiscrepancyCommand command = new ResolveDiscrepancyCommand(
                actor,
                discrepancyId,
                DiscrepancyViews.resolutionOf(request.getResolution()),
                request.getNote(),
                liableParty);
        return ResponseEntity.ok(DiscrepancyViews.toView(resolveDiscrepancy.resolveDiscrepancy(command), actor));
    }
}
