package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.application.port.in.GetDashboardUseCase;
import com.yarnshipment.reporting.application.port.in.ListShipmentReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListSubcontractorReportUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.ReportQueryPort;
import com.yarnshipment.reporting.domain.model.BandCounts;
import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.OpenDiscrepancies;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.QuantitySummary;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.ShipmentTotals;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard ve rapor okumaları. Kapsam dışı kullanıcı boş rapor alır; dönem hatası fırlatılır. */
@Service
public class ReportQueryService
        implements GetDashboardUseCase,
                ListSubcontractorReportUseCase,
                ListShipmentReportUseCase,
                ListSubcontractorBalanceUseCase {

    /** Döküm bir seferde bu kadar satırla sınırlıdır; dönem daraltılarak daha fazlasına ulaşılır. */
    public static final int SHIPMENT_REPORT_LIMIT = 5000;

    private final ReportQueryPort queryPort;
    private final ReportAccess access;
    private final Clock clock;

    ReportQueryService(ReportQueryPort queryPort, ReportAccess access, Clock clock) {
        this.queryPort = queryPort;
        this.access = access;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Dashboard getDashboard(ReportQuery query) {
        ReportPeriod period = periodOf(query);
        return access.scopeOf(query)
                .map(scope -> queryPort.findDashboard(period, scope, clock.instant()))
                .orElseGet(() -> emptyDashboard(period));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartyStats> listSubcontractorReport(ReportQuery query) {
        ReportPeriod period = periodOf(query);
        return access.scopeOf(query)
                .map(scope -> queryPort.findSubcontractorStats(period, scope))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentRow> listShipmentReport(ReportQuery query) {
        ReportPeriod period = periodOf(query);
        return access.scopeOf(query)
                .map(scope -> queryPort.findShipments(period, scope, SHIPMENT_REPORT_LIMIT))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcontractorBalance> listSubcontractorBalance(ReportQuery query) {
        ReportPeriod period = periodOf(query);
        return access.scopeOf(query)
                .map(scope -> queryPort.findSubcontractorBalances(period, scope))
                .orElseGet(List::of);
    }

    private ReportPeriod periodOf(ReportQuery query) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ReportPeriod.ZONE);
        return ReportPeriod.of(query.from(), query.to(), today);
    }

    private static Dashboard emptyDashboard(ReportPeriod period) {
        return new Dashboard(
                period,
                new ShipmentTotals(0, 0, 0, 0, 0),
                new QuantitySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0),
                new BandCounts(0, 0, 0),
                new OpenDiscrepancies(0, 0, 0, 0),
                List.of(),
                List.of());
    }
}
