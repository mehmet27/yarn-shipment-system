package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery.Filter;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.domain.model.Comparison;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DispatchFigures;
import com.yarnshipment.discrepancy.domain.model.DocumentFigures;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import com.yarnshipment.discrepancy.domain.model.Severity;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import java.time.Instant;
import java.util.UUID;

/** Gerçek şemaya iki uyuşmazlık açar: çıkış tartısı yok (Kırmızı) ve imzalı belge yok (Sarı). */
final class SeededDiscrepancies {

    static final UUID SITE = TestComparisons.SITE;

    static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-24T09:00:00Z");
    private static final int PAGE_SIZE = 20;
    private static final Instant DUE = Instant.parse("2026-10-30T09:00:00Z");

    private SeededDiscrepancies() {}

    static void seed(ComparisonAdapter comparisons) {
        ComparisonInput input = TestComparisons.input(
                new DispatchFigures(null, TestComparisons.BAGS, TestComparisons.SEAL),
                TestComparisons.delivery(TestComparisons.DISPATCH_KG),
                TestComparisons.KEYS,
                DocumentFigures.NONE);
        comparisons.saveComparison(new CompletedComparison(
                input, Comparison.compare(input, ToleranceBands.REPORT_DEFAULT), NOW, DUE, ACTOR));
    }

    static Discrepancy find(DiscrepancyQueryAdapter queries, Severity severity) {
        return queries.findDiscrepancies(new Filter(null, severity, null, null), AccessScope.UNRESTRICTED, 0, PAGE_SIZE)
                .items()
                .getFirst();
    }

    static Discrepancy.Decision decision() {
        return new Discrepancy.Decision(Resolution.TOLERANCE, "uygun", null, ACTOR, LATER);
    }
}
