package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportQueryPort;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** {@link ReportQueryPort}'un JDBC uygulaması; sorgular okuyucu sınıflara dağıtılır. */
@Component
final class ReportQueryAdapter implements ReportQueryPort {

    private final DashboardReader dashboardReader;
    private final DiscrepancyReader discrepancyReader;
    private final PartyReader partyReader;

    ReportQueryAdapter(DashboardReader dashboardReader, DiscrepancyReader discrepancyReader, PartyReader partyReader) {
        this.dashboardReader = dashboardReader;
        this.discrepancyReader = discrepancyReader;
        this.partyReader = partyReader;
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
}
