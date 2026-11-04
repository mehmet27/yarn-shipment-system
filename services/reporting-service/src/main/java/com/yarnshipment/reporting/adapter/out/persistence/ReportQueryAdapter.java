package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportQueryPort;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** {@link ReportQueryPort}'un JDBC uygulaması; sorgular okuyucu sınıflara dağıtılır. */
@Component
final class ReportQueryAdapter implements ReportQueryPort {

    private final DashboardReader dashboardReader;
    private final DiscrepancyReader discrepancyReader;
    private final PartyReader partyReader;
    private final ShipmentListReader shipmentListReader;

    ReportQueryAdapter(
            DashboardReader dashboardReader,
            DiscrepancyReader discrepancyReader,
            PartyReader partyReader,
            ShipmentListReader shipmentListReader) {
        this.dashboardReader = dashboardReader;
        this.discrepancyReader = discrepancyReader;
        this.partyReader = partyReader;
        this.shipmentListReader = shipmentListReader;
    }

    @Override
    public Dashboard findDashboard(ReportPeriod period, ReportScope scope, Instant now) {
        return new Dashboard(
                period,
                dashboardReader.findTotals(period, scope),
                dashboardReader.findQuantity(period, scope),
                dashboardReader.findBands(period, scope),
                discrepancyReader.findOpen(scope, now),
                discrepancyReader.findOverdue(scope, now),
                dashboardReader.findLargest(period, scope));
    }

    @Override
    public List<PartyStats> findDriverStats(ReportPeriod period, ReportScope scope) {
        return partyReader.findDriverStats(period, scope);
    }

    @Override
    public List<PartyStats> findSubcontractorStats(ReportPeriod period, ReportScope scope) {
        return partyReader.findSubcontractorStats(period, scope);
    }

    @Override
    public List<PairStats> findPairStats(ReportPeriod period, ReportScope scope) {
        return partyReader.findPairStats(period, scope);
    }

    @Override
    public List<ShipmentRow> findShipments(ReportPeriod period, ReportScope scope, int limit) {
        return shipmentListReader.findShipments(period, scope, limit);
    }

    @Override
    public List<SubcontractorBalance> findSubcontractorBalances(ReportPeriod period, ReportScope scope) {
        return shipmentListReader.findSubcontractorBalances(period, scope);
    }
}
