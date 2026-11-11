package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.util.UUID;

/** Uyuşmazlık olayları test verisi. */
final class ComparisonFixtures {

    private static final long DUE_SECONDS = 3600;

    private ComparisonFixtures() {}

    static ReadModelChange.DiscrepancyOpened opened(UUID discrepancyId, UUID shipmentId, UUID site) {
        return new ReadModelChange.DiscrepancyOpened(
                discrepancyId,
                "UY-1",
                shipmentId,
                site,
                "QUALITY",
                "RED",
                ShipmentFixtures.AT.plusSeconds(DUE_SECONDS),
                ShipmentFixtures.AT);
    }

    static ReadModelChange.DiscrepancyResolved resolved(UUID discrepancyId, UUID shipmentId) {
        return new ReadModelChange.DiscrepancyResolved(discrepancyId, shipmentId, ShipmentFixtures.AT, 0);
    }
}
