package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Her değişiklik türünden bir örnek. */
final class AllChanges {

    private AllChanges() {}

    static List<ReadModelChange> every(UUID shipmentId) {
        Instant at = Instant.parse("2026-11-03T08:00:00Z");
        UUID id = UUID.randomUUID();
        return List.of(
                TestChanges.created(shipmentId, id, id),
                new ReadModelChange.ShipmentDispatched(shipmentId, id, at),
                new ReadModelChange.ShipmentArrived(shipmentId, id),
                new ReadModelChange.ShipmentCompleted(shipmentId, at),
                new ReadModelChange.ShipmentCancelled(shipmentId),
                TestChanges.delivery(shipmentId),
                TestChanges.comparedGreen(shipmentId, at),
                new ReadModelChange.DiscrepancyOpened(id, "UY-1", shipmentId, id, "QUANTITY", "RED", at, at),
                new ReadModelChange.DiscrepancyResolved(id, shipmentId, at, 0));
    }
}
