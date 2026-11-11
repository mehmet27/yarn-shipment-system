package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistence adapter'ı gelene kadar port davranışını taklit eder; son istenen kapsamı saklar. */
final class InMemoryShipmentQueryPort implements ShipmentQueryPort {

    private final List<Shipment> shipments = new ArrayList<>();
    private AccessScope lastScope = AccessScope.UNRESTRICTED;

    void add(Shipment shipment) {
        shipments.add(shipment);
    }

    AccessScope lastScope() {
        return lastScope;
    }

    @Override
    public Optional<Shipment> findShipment(UUID shipmentId) {
        return shipments.stream()
                .filter(shipment -> shipment.id().equals(shipmentId))
                .findFirst();
    }

    @Override
    public ShipmentSlice findShipments(AccessScope scope, Set<ShipmentStatus> statuses, PageRequest page) {
        lastScope = scope;
        List<Shipment> matching = shipments.stream()
                .filter(shipment -> statuses.isEmpty() || statuses.contains(shipment.status()))
                .filter(shipment -> isOwnedWithin(scope, shipment))
                .toList();
        List<Shipment> pageContent = matching.stream()
                .skip((long) page.number() * page.size())
                .limit(page.size())
                .toList();
        return new ShipmentSlice(pageContent, matching.size());
    }

    private static boolean isOwnedWithin(AccessScope scope, Shipment shipment) {
        UUID subcontractorId = scope.subcontractorId();
        return subcontractorId == null || Objects.equals(subcontractorId, shipment.subcontractorId());
    }
}
