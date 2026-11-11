package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockOverview;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Merkez iplik stoku yanıtları; kilolar ondalık dize olarak gider (API-03). */
final class CentralStockViews {

    private CentralStockViews() {}

    record CentralStockRowView(String yarnType, String stockKg) {}

    record CentralStockMovementView(
            UUID id,
            String yarnType,
            String kind,
            String netKg,
            @Nullable String note,
            Instant occurredAt) {}

    record CentralStockView(List<CentralStockRowView> rows, String totalKg, List<CentralStockMovementView> movements) {}

    record CentralProductionView(
            UUID id, String fabricType, String netKg, String fireRate, List<CentralStockMovementView> consumptions) {}

    static CentralStockView stock(CentralStockOverview overview) {
        return new CentralStockView(
                overview.rows().stream()
                        .map(row -> new CentralStockRowView(row.yarnType(), BalanceViews.kg(row.stockKg())))
                        .toList(),
                BalanceViews.kg(overview.totalKg()),
                overview.recent().stream().map(CentralStockViews::movement).toList());
    }

    static CentralStockMovementView movement(CentralStockMovement movement) {
        return new CentralStockMovementView(
                movement.id(),
                movement.yarnType(),
                movement.kind().name(),
                BalanceViews.kg(movement.netKg()),
                movement.note(),
                movement.occurredAt());
    }

    static CentralProductionView production(CentralProduction production) {
        return new CentralProductionView(
                production.id(),
                production.fabricType(),
                BalanceViews.kg(production.netKg()),
                production.fireRate().toPlainString(),
                production.consumptions().stream()
                        .map(CentralStockViews::movement)
                        .toList());
    }
}
