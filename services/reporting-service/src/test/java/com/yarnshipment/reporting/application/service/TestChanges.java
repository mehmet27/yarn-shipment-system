package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Okuma modeli değişikliği test verileri. */
final class TestChanges {

    private static final int TWO_DISCREPANCIES = 2;

    private TestChanges() {}

    static ReadModelChange.ShipmentCreated created(UUID shipmentId, UUID siteId, UUID subcontractorId) {
        return new ReadModelChange.ShipmentCreated(
                shipmentId, "SHP-1", siteId, subcontractorId, Instant.parse("2026-11-03T08:00:00Z"));
    }

    /** Kırmızı bantta, 40 kg eksik, iki uyuşmazlıklı karşılaştırma. */
    static ReadModelChange.ComparisonCompleted comparedRed(UUID shipmentId, Instant at) {
        return new ReadModelChange.ComparisonCompleted(
                shipmentId, at, "RED", new BigDecimal("-40.000"), new BigDecimal("2.00"), TWO_DISCREPANCIES);
    }

    static ReadModelChange.ComparisonCompleted comparedGreen(UUID shipmentId, Instant at) {
        return new ReadModelChange.ComparisonCompleted(shipmentId, at, "GREEN", null, null, 0);
    }

    static ReadModelChange.DeliveryReceived delivery(UUID shipmentId) {
        return new ReadModelChange.DeliveryReceived(shipmentId, null, BigDecimal.TEN);
    }
}
