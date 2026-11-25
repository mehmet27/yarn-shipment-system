package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.FabricYarn;
import com.yarnshipment.shipment.domain.fabric.YarnDraw;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Kumaş girişi ve iplik dönüşü yanıtları. */
final class FabricViews {

    private FabricViews() {}

    record DrawView(UUID shipmentId, @Nullable String shipmentNo, String netKg) {}

    record YarnShareView(
            String yarnType,
            @Nullable UUID lotId,
            @Nullable String lotNo,
            String ratioPct,
            String netKg,
            List<DrawView> draws) {}

    record FabricReceiptView(
            UUID id,
            String fabricType,
            String netKg,
            @Nullable Integer rollCount,
            String waybillNo,
            Instant receivedAt,
            List<YarnShareView> yarns) {}

    record YarnReturnView(
            UUID id,
            String direction,
            String yarnType,
            @Nullable UUID lotId,
            String netKg,
            @Nullable String waybillNo,
            Instant createdAt) {}

    static FabricReceiptView fabricReceipt(FabricReceipt receipt) {
        return new FabricReceiptView(
                receipt.id(),
                receipt.fabricType(),
                BalanceViews.kg(receipt.netKg()),
                receipt.rollCount(),
                receipt.waybillNo(),
                receipt.receivedAt(),
                receipt.yarns().stream().map(FabricViews::yarn).toList());
    }

    static YarnShareView yarn(FabricYarn yarn) {
        return new YarnShareView(
                yarn.yarnType(),
                yarn.lotId(),
                yarn.lotNo(),
                yarn.ratioPct().toPlainString(),
                BalanceViews.kg(yarn.netKg()),
                yarn.draws().stream().map(FabricViews::draw).toList());
    }

    static DrawView draw(YarnDraw draw) {
        return new DrawView(draw.shipmentId(), draw.shipmentNo(), BalanceViews.kg(draw.netKg()));
    }

    static YarnReturnView yarnReturn(YarnReturn record) {
        return new YarnReturnView(
                record.id(),
                record.direction().name(),
                record.yarnType(),
                record.lotId(),
                BalanceViews.kg(record.netKg()),
                record.waybillNo(),
                record.createdAt());
    }
}
