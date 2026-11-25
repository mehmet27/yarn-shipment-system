package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.YarnRatio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Kumaş girişinin düşüm önizlemesi isteği; hiçbir şey kaydetmez.
 *
 * @param actor isteyen (SEC-04)
 * @param shipmentId kumaşın girileceği sevkiyat
 * @param netKg kumaş net kg
 * @param yarns kumaşın iplik oranları
 */
public record PreviewFabricReceiptCommand(Actor actor, UUID shipmentId, BigDecimal netKg, List<YarnRatio> yarns) {

    public PreviewFabricReceiptCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(netKg, "netKg");
        yarns = List.copyOf(yarns);
    }
}
