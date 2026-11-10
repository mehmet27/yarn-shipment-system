package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kumaş girişi isteği.
 *
 * @param actor kaydeden (SEC-04)
 * @param shipmentId sevkiyat
 * @param fabricType kumaş cinsi
 * @param netKg kumaş net kg
 * @param rollCount top/rulo sayısı
 * @param waybillNo kumaş irsaliye numarası
 * @param receivedAt geliş zamanı; boşsa sunucu zamanı
 * @param yarns tükettiği iplikler; boşsa sevkiyatın reçete oranlarıyla dağıtılır
 */
public record RecordFabricReceiptCommand(
        Actor actor,
        UUID shipmentId,
        String fabricType,
        BigDecimal netKg,
        @Nullable Integer rollCount,
        String waybillNo,
        @Nullable Instant receivedAt,
        @Nullable List<YarnShare> yarns) {

    public RecordFabricReceiptCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(fabricType, "fabricType");
        Objects.requireNonNull(netKg, "netKg");
        Objects.requireNonNull(waybillNo, "waybillNo");
        yarns = yarns == null || yarns.isEmpty() ? null : List.copyOf(yarns);
    }
}
