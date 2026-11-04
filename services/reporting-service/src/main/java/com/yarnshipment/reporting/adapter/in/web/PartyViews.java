package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.adapter.in.web.api.model.PairStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.PartyStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.ShipmentRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.SubcontractorBalanceView;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;

/** Şoför, fasoncu ve eşleşme göstergelerini sözleşme tiplerine çevirir. */
final class PartyViews {

    private PartyViews() {}

    static PartyStatsView toView(PartyStats stats) {
        return new PartyStatsView(
                stats.id(),
                stats.shipmentCount(),
                stats.discrepantShipmentCount(),
                stats.discrepancyRate().toPlainString(),
                stats.averageAbsoluteDiffKg().toPlainString());
    }

    static PairStatsView toView(PairStats stats) {
        return new PairStatsView(
                stats.driverId(),
                stats.subcontractorId(),
                stats.shipmentCount(),
                stats.discrepantShipmentCount(),
                stats.discrepancyCount(),
                stats.discrepancyRate().toPlainString(),
                stats.averageAbsoluteDiffKg().toPlainString());
    }

    static SubcontractorBalanceView toView(SubcontractorBalance balance) {
        return new SubcontractorBalanceView(
                balance.subcontractorId(),
                balance.shipmentCount(),
                balance.dispatchedKg().toPlainString(),
                balance.deliveredKg().toPlainString(),
                balance.inTransitKg().toPlainString(),
                balance.differenceKg().toPlainString());
    }

    static ShipmentRowView toView(ShipmentRow row) {
        var view = new ShipmentRowView(
                row.shipmentId(),
                row.shipmentNo(),
                row.subcontractorId(),
                row.status(),
                row.hadDiscrepancy(),
                row.createdAt().atOffset(ZoneOffset.UTC));
        var comparison = row.comparison();
        view.setDriverId(row.driverId());
        view.setDispatchNetKg(plain(comparison.dispatchNetKg()));
        view.setDeliveryNetKg(plain(comparison.deliveryNetKg()));
        view.setDiffKg(plain(comparison.diffKg()));
        view.setDiffPct(plain(comparison.diffPct()));
        if (comparison.band() != null) {
            view.setBand(ShipmentRowView.BandEnum.fromValue(comparison.band()));
        }
        if (row.completedAt() != null) {
            view.setCompletedAt(row.completedAt().atOffset(ZoneOffset.UTC));
        }
        return view;
    }

    private static @Nullable String plain(@Nullable BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
