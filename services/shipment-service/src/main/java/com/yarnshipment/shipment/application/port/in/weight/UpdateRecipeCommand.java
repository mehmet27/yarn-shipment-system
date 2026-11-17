package com.yarnshipment.shipment.application.port.in.weight;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Kantar uyuşmazlığındaki sevkiyatın reçetesini düzeltme isteği.
 *
 * @param actor müdür ya da yönetici
 * @param shipmentId WEIGHT_MISMATCH durumundaki sevkiyat
 * @param lines düzeltilmiş reçete satırları; sıra satır numarasıdır, mevcut satırlardan az olamaz
 * @param reason düzeltme gerekçesi, zorunlu
 */
public record UpdateRecipeCommand(Actor actor, UUID shipmentId, List<RecipeLine> lines, String reason) {

    public UpdateRecipeCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(reason, "reason");
        lines = List.copyOf(lines);
    }
}
