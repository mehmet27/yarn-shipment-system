package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.Weighing;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Bir sayfadaki sevkiyatların geçerli çıkış tartımları, sevkiyata göre gruplanmış. */
final class ShipmentSides {

    static final String DISPATCH = "DISPATCH";

    private final Map<UUID, WeighingJpaEntity> dispatchWeighings;

    ShipmentSides(List<WeighingJpaEntity> weighings) {
        this.dispatchWeighings = weighings.stream()
                .filter(w -> DISPATCH.equals(w.getSide()))
                .collect(Collectors.toMap(WeighingJpaEntity::getShipmentId, Function.identity()));
    }

    @Nullable
    Weighing dispatchWeighing(UUID shipmentId) {
        WeighingJpaEntity weighing = dispatchWeighings.get(shipmentId);
        return weighing == null ? null : toWeighing(weighing);
    }

    private static Weighing toWeighing(WeighingJpaEntity entity) {
        return new Weighing(entity.getGrossKg(), entity.getTareKg(), entity.getBagCount(), entity.getWeighedAt());
    }
}
