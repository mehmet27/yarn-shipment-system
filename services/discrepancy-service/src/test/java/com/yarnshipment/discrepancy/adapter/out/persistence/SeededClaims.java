package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.Comparison;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Gerçek şemaya temiz bir teslim ve üzerinde UK-0001 için fotoğraflı bir kalite itirazı yazar. */
final class SeededClaims {

    static final UUID ACTOR = SeededDiscrepancies.ACTOR;
    static final UUID PHOTO = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0003");
    static final Instant OPENED_AT = Instant.parse("2026-10-26T09:00:00Z");
    private static final Instant DUE = Instant.parse("2026-11-02T09:00:00Z");

    private SeededClaims() {}

    static void deliver(ComparisonAdapter comparisons) {
        comparisons.saveComparison(new CompletedComparison(
                TestComparisons.clean(),
                Comparison.compare(TestComparisons.clean(), ToleranceBands.REPORT_DEFAULT),
                TestComparisons.RECEIVED_AT,
                DUE,
                ACTOR));
    }

    static QualityClaim open(QualityClaimAdapter claims) {
        ClaimTarget target = claims.findTarget(TestComparisons.SHIPMENT).orElseThrow();
        return claims.saveClaim(new NewQualityClaim(
                target, new ClaimRequest("B kalite çıktı", List.of("UK-0001"), List.of(PHOTO)), ACTOR, OPENED_AT, DUE));
    }
}
