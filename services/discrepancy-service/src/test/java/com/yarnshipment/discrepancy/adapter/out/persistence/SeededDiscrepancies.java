package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery.Filter;
import com.yarnshipment.discrepancy.domain.model.ComparisonRule;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Gerçek şemaya iki uyuşmazlık açar: kalite itirazı (Kırmızı) ve hafif kalite itirazı (Sarı). */
final class SeededDiscrepancies {

    static final UUID SITE = TestComparisons.SITE;

    static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-24T09:00:00Z");
    private static final int PAGE_SIZE = 20;
    private static final Instant DUE = Instant.parse("2026-10-30T09:00:00Z");

    private SeededDiscrepancies() {}

    static void seed(ComparisonSeeder comparisons) {
        comparisons.seed(
                List.of(
                        Finding.of(ComparisonRule.QUALITY_CLAIM, null, TestComparisons.DISPATCH_KG),
                        Finding.of(ComparisonRule.QUALITY_CLAIM_MINOR, null, null)),
                NOW,
                DUE);
    }

    static Discrepancy find(DiscrepancyQueryAdapter queries, Severity severity) {
        return queries.findDiscrepancies(
                        new Filter(null, severity, null, null, null), AccessScope.UNRESTRICTED, 0, PAGE_SIZE)
                .items()
                .getFirst();
    }

    static Discrepancy.Decision decision() {
        return new Discrepancy.Decision(Resolution.TOLERANCE, "uygun", null, ACTOR, LATER);
    }
}
