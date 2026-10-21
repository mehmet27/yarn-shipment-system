package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** {@link ShipmentQueryPort}'un JPA uygulaması. Tartım ve mühürler sayfa başına tek sorguyla toplu okunur. */
@Component
final class ShipmentPersistenceAdapter implements ShipmentQueryPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ShipmentJpaRepository shipments;
    private final WeighingJpaRepository weighings;
    private final SealJpaRepository seals;

    ShipmentPersistenceAdapter(
            ShipmentJpaRepository shipments, WeighingJpaRepository weighings, SealJpaRepository seals) {
        this.shipments = shipments;
        this.weighings = weighings;
        this.seals = seals;
    }

    @Override
    public Optional<Shipment> findShipment(UUID shipmentId) {
        return shipments
                .findById(shipmentId)
                .map(entity -> toDomain(List.of(entity)).getFirst());
    }

    @Override
    public ShipmentSlice findShipments(AccessScope scope, Set<ShipmentStatus> statuses, PageRequest page) {
        Page<ShipmentJpaEntity> result = shipments.findAll(
                ShipmentSpecifications.inScopeWithStatus(scope, statuses),
                org.springframework.data.domain.PageRequest.of(page.number(), page.size(), NEWEST_FIRST));
        return new ShipmentSlice(toDomain(result.getContent()), result.getTotalElements());
    }

    private List<Shipment> toDomain(List<ShipmentJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = entities.stream().map(ShipmentJpaEntity::getId).toList();
        ShipmentSides sides = new ShipmentSides(
                weighings.findByShipmentIdInAndSupersededByIsNull(ids),
                seals.findByShipmentIdInAndSupersededByIsNull(ids));
        return entities.stream()
                .map(entity -> ShipmentMapper.toDomain(entity, sides))
                .toList();
    }
}
