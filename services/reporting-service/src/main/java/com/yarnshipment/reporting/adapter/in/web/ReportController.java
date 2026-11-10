package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.reporting.adapter.in.web.api.ReportsApi;
import com.yarnshipment.reporting.adapter.in.web.api.model.BalanceMatrixRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.DashboardView;
import com.yarnshipment.reporting.adapter.in.web.api.model.FireRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.OpeningImportRequest;
import com.yarnshipment.reporting.adapter.in.web.api.model.OpeningImportResult;
import com.yarnshipment.reporting.adapter.in.web.api.model.PartyStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.ShipmentRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.SubcontractorBalanceView;
import com.yarnshipment.reporting.adapter.in.web.api.model.YarnVsFabricRowView;
import com.yarnshipment.reporting.application.port.in.BalanceFireReportUseCase;
import com.yarnshipment.reporting.application.port.in.GetDashboardUseCase;
import com.yarnshipment.reporting.application.port.in.ImportOpeningBalanceUseCase;
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
    private final ListSubcontractorReportUseCase listSubcontractors;
    private final ListShipmentReportUseCase listShipments;
    private final ListSubcontractorBalanceUseCase listBalances;
    private final BalanceFireReportUseCase balanceFire;
    private final ImportOpeningBalanceUseCase importOpening;
    private final CurrentActorProvider currentActorProvider;

    public ReportController(
            GetDashboardUseCase getDashboard,
            ListSubcontractorReportUseCase listSubcontractors,
            ListShipmentReportUseCase listShipments,
            ListSubcontractorBalanceUseCase listBalances,
            BalanceFireReportUseCase balanceFire,
            ImportOpeningBalanceUseCase importOpening,
            CurrentActorProvider currentActorProvider) {
        this.getDashboard = getDashboard;
        this.listSubcontractors = listSubcontractors;
        this.listShipments = listShipments;
        this.listBalances = listBalances;
        this.balanceFire = balanceFire;
        this.importOpening = importOpening;
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
    public ResponseEntity<List<PartyStatsView>> getSubcontractorReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listSubcontractors.listSubcontractorReport(query(from, to, siteId)).stream()
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

    @Override
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<List<BalanceMatrixRowView>> getBalanceMatrix(@Nullable UUID siteId) {
        return ResponseEntity.ok(balanceFire.listBalanceMatrix(query(null, null, siteId)).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<FireRowView>> getFireReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId, @Nullable UUID subcontractorId) {
        return ResponseEntity.ok(balanceFire.listFire(query(from, to, siteId), subcontractorId).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<YarnVsFabricRowView>> getYarnVsFabricReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(balanceFire.listYarnVsFabric(query(from, to, siteId)).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<FireRowView>> getToInvoiceReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(balanceFire.listToInvoice(query(from, to, siteId)).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<OpeningImportResult> importOpeningBalance(OpeningImportRequest request) {
        var result = importOpening.importOpening(
                request.getAsOf(),
                request.getRows().stream().map(BalanceFireViews::toDomain).toList());
        return ResponseEntity.ok(new OpeningImportResult(result.imported(), result.skipped()));
    }

    private ReportQuery query(@Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return new ReportQuery(currentActorProvider.getCurrentActor(), from, to, siteId);
    }
}
