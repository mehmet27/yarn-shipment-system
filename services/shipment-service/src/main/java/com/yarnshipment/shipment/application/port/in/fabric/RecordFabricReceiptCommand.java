package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.YarnRatio;
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
 * @param yarns kumaşın iplik oranları; toplamı 100 olmalı, iplikler fasoncunun sevkiyatlarından düşer
 */
public record RecordFabricReceiptCommand(
        Actor actor,
        UUID shipmentId,
        String fabricType,
        BigDecimal netKg,
        @Nullable Integer rollCount,
        String waybillNo,
        @Nullable Instant receivedAt,
        List<YarnRatio> yarns) {

    public RecordFabricReceiptCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(fabricType, "fabricType");
        Objects.requireNonNull(netKg, "netKg");
        Objects.requireNonNull(waybillNo, "waybillNo");
        Objects.requireNonNull(yarns, "yarns");
        yarns = List.copyOf(yarns);
    }
}
