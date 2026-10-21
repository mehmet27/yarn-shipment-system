package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.SealCondition;
import com.yarnshipment.shipment.domain.model.ShipmentSide;
import com.yarnshipment.shipment.domain.model.Weighing;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Bir sayfadaki sevkiyatların geçerli tartım ve mühürleri, sevkiyat ve tarafa göre gruplanmış. */
final class ShipmentSides {

    static final String DISPATCH = "DISPATCH";
    static final String DELIVERY = "DELIVERY";

    private final Map<SideKey, WeighingJpaEntity> weighings;
    private final Map<SideKey, SealJpaEntity> seals;

    ShipmentSides(List<WeighingJpaEntity> weighings, List<SealJpaEntity> seals) {
        this.weighings = weighings.stream()
                .collect(Collectors.toMap(w -> new SideKey(w.getShipmentId(), w.getSide()), Function.identity()));
        this.seals = seals.stream()
                .collect(Collectors.toMap(s -> new SideKey(s.getShipmentId(), s.getSide()), Function.identity()));
    }

    ShipmentSide side(UUID shipmentId, String side) {
        SideKey key = new SideKey(shipmentId, side);
        WeighingJpaEntity weighing = weighings.get(key);
        SealJpaEntity seal = seals.get(key);
        if (weighing == null && seal == null) {
            return ShipmentSide.EMPTY;
        }
        return new ShipmentSide(
                weighing == null ? null : toWeighing(weighing),
                seal == null || seal.getSealNo().isEmpty() ? null : seal.getSealNo(),
                seal == null ? null : SealCondition.valueOf(seal.getSealCondition()));
    }

    private static Weighing toWeighing(WeighingJpaEntity entity) {
        return new Weighing(entity.getGrossKg(), entity.getTareKg(), entity.getBagCount(), entity.getWeighedAt());
    }

    private record SideKey(UUID shipmentId, String side) {}
}
