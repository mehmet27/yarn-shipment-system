package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.time.Instant;
import java.util.UUID;

/** Okuma modeli değişikliği test verileri. */
final class TestChanges {

    private TestChanges() {}

    static ReadModelChange.ShipmentCreated created(UUID shipmentId, UUID siteId, UUID subcontractorId) {
        return new ReadModelChange.ShipmentCreated(
                shipmentId, "SHP-1", siteId, subcontractorId, Instant.parse("2026-11-03T08:00:00Z"));
    }
}
