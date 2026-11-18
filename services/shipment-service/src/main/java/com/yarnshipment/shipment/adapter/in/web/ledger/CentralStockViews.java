package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockOverview;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockEntry;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.ReceiptDetails;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Merkez iplik stoku yanıtları; kilolar ondalık dize olarak gider (API-03). */
final class CentralStockViews {

    private CentralStockViews() {}

    record CentralStockRowView(@Nullable UUID productId, String yarnType, String stockKg) {}

    record CentralStockMovementView(
            UUID id,
            @Nullable UUID productId,
            String yarnType,
            String kind,
            String netKg,
            @Nullable String note,
            Instant occurredAt,
            @Nullable ReceiptView receipt) {}

    /** İplik girişinin tedarikçi bilgisi. */
    record ReceiptView(
            @Nullable UUID supplierId,
            String supplierName,
            @Nullable UUID lotId,
            @Nullable String lotNo,
            @Nullable String waybillNo) {}

    record SupplierReceiptView(
            @Nullable UUID supplierId,
            String supplierName,
            @Nullable UUID productId,
            String yarnType,
            String receivedKg) {}

    record YarnFlowView(
            String receivedKg, String directTransferKg, String atSubcontractorsKg, String usedInCentralProductionKg) {}

    record CentralStockView(
            List<CentralStockRowView> rows,
            String totalKg,
            String producedFabricKg,
            YarnFlowView flow,
            List<CentralStockMovementView> movements,
            List<SupplierReceiptView> suppliers) {}

    record CentralProductionView(
            UUID id, String fabricType, String netKg, String fireRate, List<CentralStockMovementView> consumptions) {}

    static CentralStockView stock(CentralStockOverview overview) {
        return new CentralStockView(
                overview.rows().stream()
                        .map(row -> new CentralStockRowView(
                                row.productId(), row.yarnType(), BalanceViews.kg(row.stockKg())))
                        .toList(),
                BalanceViews.kg(overview.totalKg()),
                BalanceViews.kg(overview.producedFabricKg()),
                new YarnFlowView(
                        BalanceViews.kg(overview.flow().receivedKg()),
                        BalanceViews.kg(overview.flow().directTransferKg()),
                        BalanceViews.kg(overview.flow().atSubcontractorsKg()),
                        BalanceViews.kg(overview.flow().usedInCentralProductionKg())),
                overview.recent().stream().map(CentralStockViews::entry).toList(),
                overview.suppliers().stream()
                        .map(row -> new SupplierReceiptView(
                                row.supplierId(),
                                row.supplierName(),
                                row.productId(),
                                row.yarnType(),
                                BalanceViews.kg(row.receivedKg())))
                        .toList());
    }

    static CentralStockMovementView entry(CentralStockEntry entry) {
        ReceiptDetails details = entry.receipt();
        return movement(
                entry.movement(),
                details == null
                        ? null
                        : new ReceiptView(
                                details.supplierId(),
                                details.supplierName(),
                                details.lotId(),
                                details.lotNo(),
                                details.waybillNo()));
    }

    static CentralStockMovementView movement(CentralStockMovement movement) {
        return movement(movement, null);
    }

    private static CentralStockMovementView movement(CentralStockMovement movement, @Nullable ReceiptView receipt) {
        return new CentralStockMovementView(
                movement.id(),
                movement.productId(),
                movement.yarnType(),
                movement.kind().name(),
                BalanceViews.kg(movement.netKg()),
                movement.note(),
                movement.occurredAt(),
                receipt);
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
