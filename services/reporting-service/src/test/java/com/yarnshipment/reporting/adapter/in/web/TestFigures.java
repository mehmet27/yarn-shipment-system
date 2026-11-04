package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.domain.model.LargestDifference;
import com.yarnshipment.reporting.domain.model.OverdueDiscrepancy;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.QuantitySummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Rapor uçları testlerinin örnek ölçüleri. */
final class TestFigures {

    static final int DRIVER_SHIPMENTS = 4;

    private TestFigures() {}

    static QuantitySummary quantity() {
        return new QuantitySummary(new BigDecimal("1000.000"), new BigDecimal("960.000"), new BigDecimal("40.000"), 2);
    }

    static LargestDifference largest() {
        return new LargestDifference(
                UUID.randomUUID(), "SHP-7", new BigDecimal("40.000"), new BigDecimal("4.00"), "RED");
    }

    static PartyStats driver() {
        return new PartyStats(UUID.randomUUID(), DRIVER_SHIPMENTS, 1, new BigDecimal("10.000"));
    }

    static PairStats pair() {
        return new PairStats(UUID.randomUUID(), UUID.randomUUID(), DRIVER_SHIPMENTS, 1, 1, new BigDecimal("10.000"));
    }

    static OverdueDiscrepancy overdue() {
        return new OverdueDiscrepancy(
                UUID.randomUUID(), "UY-1", UUID.randomUUID(), "RED", Instant.parse("2026-11-01T00:00:00Z"));
    }
}
