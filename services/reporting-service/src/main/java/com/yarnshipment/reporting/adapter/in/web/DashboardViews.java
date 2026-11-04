package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.adapter.in.web.api.model.BandCountsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.DashboardView;
import com.yarnshipment.reporting.adapter.in.web.api.model.LargestDifferenceView;
import com.yarnshipment.reporting.adapter.in.web.api.model.OpenDiscrepanciesView;
import com.yarnshipment.reporting.adapter.in.web.api.model.OverdueDiscrepancyView;
import com.yarnshipment.reporting.adapter.in.web.api.model.QuantityView;
import com.yarnshipment.reporting.adapter.in.web.api.model.ShipmentTotalsView;
import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.LargestDifference;
import com.yarnshipment.reporting.domain.model.OverdueDiscrepancy;
import java.time.ZoneOffset;

/** Dashboard modelini sözleşme tipine çevirir. Kilo ve yüzdeler ondalık dizedir (API-03). */
final class DashboardViews {

    private DashboardViews() {}

    static DashboardView toView(Dashboard dashboard) {
        var shipments = dashboard.shipments();
        var quantity = dashboard.quantity();
        var bands = dashboard.bands();
        var open = dashboard.discrepancies();
        return new DashboardView(
                dashboard.period().from(),
                dashboard.period().to(),
                new ShipmentTotalsView(
                        shipments.total(),
                        shipments.inProgress(),
                        shipments.inReview(),
                        shipments.completed(),
                        shipments.cancelled()),
                new QuantityView(
                        quantity.dispatchNetKg().toPlainString(),
                        quantity.deliveryNetKg().toPlainString(),
                        quantity.absoluteDiffKg().toPlainString(),
                        quantity.comparedShipments()),
                new BandCountsView(bands.green(), bands.yellow(), bands.red()),
                new OpenDiscrepanciesView(open.open(), open.red(), open.yellow(), open.overdue()),
                dashboard.overdueDiscrepancies().stream()
                        .map(DashboardViews::toView)
                        .toList(),
                dashboard.largestDifferences().stream()
                        .map(DashboardViews::toView)
                        .toList());
    }

    private static OverdueDiscrepancyView toView(OverdueDiscrepancy overdue) {
        return new OverdueDiscrepancyView(
                overdue.discrepancyId(),
                overdue.discrepancyNo(),
                overdue.shipmentId(),
                OverdueDiscrepancyView.SeverityEnum.fromValue(overdue.severity()),
                overdue.dueAt().atOffset(ZoneOffset.UTC));
    }

    private static LargestDifferenceView toView(LargestDifference difference) {
        var view = new LargestDifferenceView(
                difference.shipmentId(),
                difference.shipmentNo(),
                difference.diffKg().toPlainString(),
                LargestDifferenceView.BandEnum.fromValue(difference.band()));
        if (difference.diffPct() != null) {
            view.setDiffPct(difference.diffPct().toPlainString());
        }
        return view;
    }
}
