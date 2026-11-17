package com.yarnshipment.shipment.application.port.in.weight;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.Recipe;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Müdürün kantar uyuşmazlığı kararı.
 *
 * @param actor karar veren müdür ya da yönetici
 * @param shipmentId WEIGHT_MISMATCH durumundaki sevkiyat
 * @param decision karar
 * @param reason gerekçe, zorunlu
 * @param lines müdürün düzelttiği iplik listesi; yalnızca {@code UPDATE_RECIPE} için zorunlu, diğer kararlarda boş
 */
public record ResolveWeightMismatchCommand(
        Actor actor, UUID shipmentId, WeightDecision decision, String reason, List<Recipe.LineWeight> lines) {

    /** İplik listesi düzeltmesi olmayan karar. */
    public ResolveWeightMismatchCommand(Actor actor, UUID shipmentId, WeightDecision decision, String reason) {
        this(actor, shipmentId, decision, reason, List.of());
    }

    public ResolveWeightMismatchCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(reason, "reason");
        lines = List.copyOf(lines);
    }
}
