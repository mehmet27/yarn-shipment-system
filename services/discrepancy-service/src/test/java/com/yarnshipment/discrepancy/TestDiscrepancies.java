package com.yarnshipment.discrepancy;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Testlerde kullanılan örnek uyuşmazlıklar. */
public final class TestDiscrepancies {

    public static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000d0001");
    public static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    public static final Instant CREATED_AT = Instant.parse("2026-10-23T09:00:00Z");
    public static final Instant DUE_AT = Instant.parse("2026-10-30T09:00:00Z");

    private TestDiscrepancies() {}

    public static Discrepancy open(UUID siteId) {
        return withStatus(siteId, DiscrepancyStatus.OPEN);
    }

    public static Discrepancy withStatus(UUID siteId, DiscrepancyStatus status) {
        return new Discrepancy(
                ID,
                "UY-SV-3-1",
                siteId,
                SHIPMENT,
                new Discrepancy.Basis(DiscrepancyType.QUANTITY, Severity.RED, "QUANTITY_RED"),
                new Discrepancy.Measure("2000.000", "1960.000", new BigDecimal("40.000"), new BigDecimal("2.00")),
                status,
                new Discrepancy.Handling(null, DUE_AT),
                null,
                CREATED_AT);
    }
}
