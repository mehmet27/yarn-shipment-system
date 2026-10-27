package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.discrepancy.adapter.in.web.api.DiscrepanciesApi;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyActivityView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyPage;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.Severity;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyUseCase;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesUseCase;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepancyActivityUseCase;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Uyuşmazlık uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class DiscrepancyController implements DiscrepanciesApi {

    private final ListDiscrepanciesUseCase listDiscrepancies;
    private final GetDiscrepancyUseCase getDiscrepancy;
    private final ListDiscrepancyActivityUseCase listActivity;
    private final CurrentActorProvider currentActorProvider;

    public DiscrepancyController(
            ListDiscrepanciesUseCase listDiscrepancies,
            GetDiscrepancyUseCase getDiscrepancy,
            ListDiscrepancyActivityUseCase listActivity,
            CurrentActorProvider currentActorProvider) {
        this.listDiscrepancies = listDiscrepancies;
        this.getDiscrepancy = getDiscrepancy;
        this.listActivity = listActivity;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_VIEW')")
    public ResponseEntity<DiscrepancyPage> listDiscrepancies(
            @Nullable DiscrepancyStatus status,
            @Nullable Severity severity,
            @Nullable UUID assigneeId,
            @Nullable UUID shipmentId,
            Integer page,
            Integer size) {
        Actor actor = currentActorProvider.getCurrentActor();
        ListDiscrepanciesQuery.Filter filter = DiscrepancyViews.filterOf(status, severity, assigneeId, shipmentId);
        var result = listDiscrepancies.listDiscrepancies(new ListDiscrepanciesQuery(actor, filter, page, size));
        return ResponseEntity.ok(DiscrepancyViews.toPage(result, actor));
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_VIEW')")
    public ResponseEntity<DiscrepancyView> getDiscrepancy(UUID discrepancyId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(DiscrepancyViews.toView(
                getDiscrepancy.getDiscrepancy(new GetDiscrepancyQuery(actor, discrepancyId)), actor));
    }

    @Override
    @PreAuthorize("hasAuthority('DISCREPANCY_VIEW')")
    public ResponseEntity<List<DiscrepancyActivityView>> listDiscrepancyActivity(UUID discrepancyId) {
        Actor actor = currentActorProvider.getCurrentActor();
        return ResponseEntity.ok(listActivity.listActivity(new GetDiscrepancyQuery(actor, discrepancyId)).stream()
                .map(DiscrepancyViews::toView)
                .toList());
    }
}
