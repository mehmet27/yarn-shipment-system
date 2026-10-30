package com.yarnshipment.discrepancy;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Testlerde kullanılan örnek uyuşmazlıklar. */
public final class TestDiscrepancies {

    public static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000d0001");
    public static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    public static final UUID SUBCONTRACTOR = TestComparisons.SUBCONTRACTOR;
    public static final Instant CREATED_AT = Instant.parse("2026-10-23T09:00:00Z");
    public static final Instant DUE_AT = Instant.parse("2026-10-30T09:00:00Z");

    private TestDiscrepancies() {}

    public static Discrepancy open(UUID siteId) {
        return withStatus(siteId, DiscrepancyStatus.OPEN);
    }

    public static Discrepancy withStatus(UUID siteId, DiscrepancyStatus status) {
        return of(siteId, SUBCONTRACTOR, status);
    }

    /** Verilen fasoncunun sevkiyatında açılmış uyuşmazlık; fasoncusu boş eski kayıt için {@code null}. */
    public static Discrepancy of(UUID siteId, @Nullable UUID subcontractorId, DiscrepancyStatus status) {
        return withBasis(
                siteId,
                subcontractorId,
                status,
                new Discrepancy.Basis(DiscrepancyType.QUANTITY, Severity.RED, "QUANTITY_RED"));
    }

    /** {@link TestTokens#SITE_ID} lokasyonunda, verilen kurala göre açılmış uyuşmazlık. */
    public static Discrepancy withRule(UUID id, DiscrepancyType type, String ruleCode, DiscrepancyStatus status) {
        Discrepancy discrepancy = withBasis(
                TestTokens.SITE_ID, SUBCONTRACTOR, status, new Discrepancy.Basis(type, Severity.YELLOW, ruleCode));
        return new Discrepancy(
                id,
                discrepancy.discrepancyNo(),
                discrepancy.origin(),
                discrepancy.basis(),
                discrepancy.measure(),
                status,
                discrepancy.handling(),
                null,
                CREATED_AT);
    }

    private static Discrepancy withBasis(
            UUID siteId, @Nullable UUID subcontractorId, DiscrepancyStatus status, Discrepancy.Basis basis) {
        return new Discrepancy(
                ID,
                "UY-SV-3-1",
                new Discrepancy.Origin(siteId, SHIPMENT, subcontractorId),
                basis,
                new Discrepancy.Measure("2000.000", "1960.000", new BigDecimal("40.000"), new BigDecimal("2.00")),
                status,
                new Discrepancy.Handling(null, DUE_AT),
                null,
                CREATED_AT);
    }
}
