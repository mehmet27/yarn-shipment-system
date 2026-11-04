package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.math.BigDecimal;
import java.util.UUID;

/** Teslim, karşılaştırma ve uyuşmazlık olayları test verisi. */
final class ComparisonFixtures {

    private static final long DUE_SECONDS = 3600;

    private ComparisonFixtures() {}

    static ReadModelChange.DeliveryReceived delivery(UUID id) {
        return new ReadModelChange.DeliveryReceived(id, new BigDecimal("500.000"), new BigDecimal("460.000"));
    }

    /** 40 kg eksik, bir uyuşmazlıklı Kırmızı karşılaştırma. */
    static ReadModelChange.ComparisonCompleted redComparison(UUID id) {
        return new ReadModelChange.ComparisonCompleted(
                id, ShipmentFixtures.AT, "RED", new BigDecimal("40.000"), new BigDecimal("8.00"), 1);
    }

    static ReadModelChange.DiscrepancyOpened opened(UUID discrepancyId, UUID shipmentId, UUID site) {
        return new ReadModelChange.DiscrepancyOpened(
                discrepancyId,
                "UY-1",
                shipmentId,
                site,
                "QUANTITY",
                "RED",
                ShipmentFixtures.AT.plusSeconds(DUE_SECONDS),
                ShipmentFixtures.AT);
    }

    static ReadModelChange.DiscrepancyResolved resolved(UUID discrepancyId, UUID shipmentId) {
        return new ReadModelChange.DiscrepancyResolved(discrepancyId, shipmentId, ShipmentFixtures.AT, 0);
    }
}
