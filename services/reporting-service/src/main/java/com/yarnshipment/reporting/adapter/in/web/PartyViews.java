package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.adapter.in.web.api.model.PartyStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.ShipmentRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.SubcontractorBalanceView;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;

/** Fasoncu göstergelerini, bakiyeyi ve sevkiyat satırlarını sözleşme tiplerine çevirir. */
final class PartyViews {

    private PartyViews() {}

    static PartyStatsView toView(PartyStats stats) {
        return new PartyStatsView(
                stats.id(),
                stats.shipmentCount(),
                stats.discrepantShipmentCount(),
                stats.discrepancyRate().toPlainString());
    }

    static SubcontractorBalanceView toView(SubcontractorBalance balance) {
        return new SubcontractorBalanceView(
                balance.subcontractorId(),
                balance.shipmentCount(),
                balance.dispatchedKg().toPlainString(),
                balance.deliveredKg().toPlainString(),
                balance.inTransitKg().toPlainString());
    }

    static ShipmentRowView toView(ShipmentRow row) {
        var view = new ShipmentRowView(
                row.shipmentId(),
                row.shipmentNo(),
                row.subcontractorId(),
                row.status(),
                row.hadDiscrepancy(),
                row.createdAt().atOffset(ZoneOffset.UTC));
        view.setDispatchNetKg(plain(row.dispatchNetKg()));
        view.setDeliveryNetKg(plain(row.deliveryNetKg()));
        if (row.completedAt() != null) {
            view.setCompletedAt(row.completedAt().atOffset(ZoneOffset.UTC));
        }
        return view;
    }

    private static @Nullable String plain(@Nullable BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
