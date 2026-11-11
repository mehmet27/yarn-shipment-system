package com.yarnshipment.shipment.adapter.in.web.ledger;

import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Kumaş girişi ve iplik dönüşü yanıtları. */
final class FabricViews {

    private FabricViews() {}

    record YarnShareView(String yarnType, String netKg) {}

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
                receipt.yarns().stream()
                        .map(share -> new YarnShareView(share.yarnType(), BalanceViews.kg(share.netKg())))
                        .toList());
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
