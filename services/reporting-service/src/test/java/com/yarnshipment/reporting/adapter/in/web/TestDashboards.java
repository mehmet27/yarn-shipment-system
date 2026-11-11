package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.OpenDiscrepancies;
import com.yarnshipment.reporting.domain.model.OverdueDiscrepancy;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentTotals;
import java.time.LocalDate;
import java.util.List;

/** Rapor uçları testlerinin örnek dashboard'u. */
final class TestDashboards {

    static final int TOTAL = 5;
    private static final int OPEN = 3;

    private TestDashboards() {}

    static Dashboard sample() {
        return build(List.of());
    }

    static Dashboard withOverdue() {
        return build(List.of(TestFigures.overdue()));
    }

    private static Dashboard build(List<OverdueDiscrepancy> overdue) {
        return new Dashboard(
                new ReportPeriod(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03")),
                new ShipmentTotals(TOTAL, 2, 1, 1, 1),
                TestFigures.quantity(),
                new OpenDiscrepancies(OPEN, 1, 2, 1),
                overdue);
    }
}
