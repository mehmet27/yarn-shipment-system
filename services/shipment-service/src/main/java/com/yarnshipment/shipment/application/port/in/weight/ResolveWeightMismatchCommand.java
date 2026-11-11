package com.yarnshipment.shipment.application.port.in.weight;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.util.Objects;
import java.util.UUID;

/**
 * Müdürün kantar uyuşmazlığı kararı.
 *
 * @param actor karar veren müdür ya da yönetici
 * @param shipmentId WEIGHT_MISMATCH durumundaki sevkiyat
 * @param decision karar
 * @param reason gerekçe, zorunlu
 */
public record ResolveWeightMismatchCommand(Actor actor, UUID shipmentId, WeightDecision decision, String reason) {

    public ResolveWeightMismatchCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(reason, "reason");
    }
}
