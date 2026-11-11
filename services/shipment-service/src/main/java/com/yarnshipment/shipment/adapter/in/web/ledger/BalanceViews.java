package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.BalanceSummary;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bakiye ve hareket yanıtları. Kilolar 3 ondalıklı dizedir (API-03). */
final class BalanceViews {

    private BalanceViews() {}

    record BalanceRowView(
            UUID subcontractorId, String subcontractorTitle, String yarnType, String balanceKg, String inTransitKg) {}

    record BalanceSummaryView(List<BalanceRowView> items, String totalKg) {}

    record MovementView(
            UUID id,
            UUID subcontractorId,
            String yarnType,
            @Nullable UUID lotId,
            @Nullable UUID shipmentId,
            String kind,
            String netKg,
            Instant occurredAt,
            UUID createdBy,
            @Nullable String reason) {}

    record MovementPageView(List<MovementView> items, PageMetadataView page) {}

    static BalanceSummaryView balances(BalanceSummary summary) {
        return new BalanceSummaryView(
                summary.items().stream().map(BalanceViews::balanceRow).toList(), kg(summary.totalKg()));
    }

    static MovementPageView movements(MovementPage page) {
        return new MovementPageView(
                page.items().stream().map(BalanceViews::movement).toList(),
                new PageMetadataView(page.number(), page.size(), page.totalElements(), page.totalPages()));
    }

    static String kg(BigDecimal value) {
        return Kilograms.round(value).toPlainString();
    }

    private static BalanceRowView balanceRow(BalanceRow row) {
        return new BalanceRowView(
                row.subcontractorId(),
                row.subcontractorTitle(),
                row.yarnType(),
                kg(row.balanceKg()),
                kg(row.inTransitKg()));
    }

    private static MovementView movement(BalanceMovement movement) {
        return new MovementView(
                movement.id(),
                movement.subcontractorId(),
                movement.yarn().yarnType(),
                movement.yarn().lotId(),
                movement.shipmentId(),
                movement.kind().name(),
                kg(movement.netKg()),
                movement.occurredAt(),
                movement.createdBy(),
                movement.reason());
    }
}
