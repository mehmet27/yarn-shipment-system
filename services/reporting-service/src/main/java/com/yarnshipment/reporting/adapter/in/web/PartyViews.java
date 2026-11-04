package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.adapter.in.web.api.model.PairStatsView;
import com.yarnshipment.reporting.adapter.in.web.api.model.PartyStatsView;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;

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
}
