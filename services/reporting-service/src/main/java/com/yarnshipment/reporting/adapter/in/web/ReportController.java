package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.reporting.adapter.in.web.api.ReportsApi;
import com.yarnshipment.reporting.adapter.in.web.api.model.DashboardView;
import com.yarnshipment.reporting.adapter.in.web.api.model.PairStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.PartyStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.ShipmentRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.SubcontractorBalanceView;
import com.yarnshipment.reporting.application.port.in.GetDashboardUseCase;
import com.yarnshipment.reporting.application.port.in.ListDriverReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListPairReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListShipmentReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorReportUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard ve rapor uçları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class ReportController implements ReportsApi {

    private final GetDashboardUseCase getDashboard;
    private final ListDriverReportUseCase listDrivers;
    private final ListSubcontractorReportUseCase listSubcontractors;
    private final ListPairReportUseCase listPairs;
    private final ListShipmentReportUseCase listShipments;
    private final ListSubcontractorBalanceUseCase listBalances;
    private final CurrentActorProvider currentActorProvider;

    public ReportController(
            GetDashboardUseCase getDashboard,
            ListDriverReportUseCase listDrivers,
            ListSubcontractorReportUseCase listSubcontractors,
            ListPairReportUseCase listPairs,
            ListShipmentReportUseCase listShipments,
            ListSubcontractorBalanceUseCase listBalances,
            CurrentActorProvider currentActorProvider) {
        this.getDashboard = getDashboard;
        this.listDrivers = listDrivers;
        this.listSubcontractors = listSubcontractors;
        this.listPairs = listPairs;
        this.listShipments = listShipments;
        this.listBalances = listBalances;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<DashboardView> getDashboard(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(DashboardViews.toView(getDashboard.getDashboard(query(from, to, siteId))));
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<PartyStatsView>> getDriverReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listDrivers.listDriverReport(query(from, to, siteId)).stream()
                .map(PartyViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<PartyStatsView>> getSubcontractorReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listSubcontractors.listSubcontractorReport(query(from, to, siteId)).stream()
                .map(PartyViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<PairStatsView>> getPairReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listPairs.listPairReport(query(from, to, siteId)).stream()
                .map(PartyViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<ShipmentRowView>> getShipmentReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listShipments.listShipmentReport(query(from, to, siteId)).stream()
                .map(PartyViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<SubcontractorBalanceView>> getSubcontractorBalanceReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listBalances.listSubcontractorBalance(query(from, to, siteId)).stream()
                .map(PartyViews::toView)
                .toList());
    }

    private ReportQuery query(@Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return new ReportQuery(currentActorProvider.getCurrentActor(), from, to, siteId);
    }
}
