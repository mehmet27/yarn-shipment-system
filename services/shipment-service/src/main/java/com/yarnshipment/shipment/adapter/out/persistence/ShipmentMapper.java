package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.PlannedLoad;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;

/** JPA entity'lerini domain modeline çevirir. */
final class ShipmentMapper {

    private ShipmentMapper() {}

    static Shipment toDomain(ShipmentJpaEntity entity, ShipmentSides sides) {
        return new Shipment(
                entity.getId(),
                entity.getShipmentNo(),
                entity.getSiteId(),
                ShipmentStatus.valueOf(entity.getStatus()),
                entity.getParties().toDomain(),
                new PlannedLoad(entity.getPlannedBagCount(), entity.getPlannedNetKg()),
                sides.side(entity.getId(), ShipmentSides.DISPATCH),
                sides.side(entity.getId(), ShipmentSides.DELIVERY),
                entity.isRandomCheckSelected(),
                entity.getCreatedAt());
    }
}
