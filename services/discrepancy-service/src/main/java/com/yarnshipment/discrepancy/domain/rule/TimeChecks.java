package com.yarnshipment.discrepancy.domain.rule;

import com.yarnshipment.discrepancy.domain.model.ComparisonRule;
import com.yarnshipment.discrepancy.domain.model.Finding;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Durumda kalma süresi (ön analiz, Bölüm 7.2: "Yolda > 24 saat" zaman anomalisidir). */
public final class TimeChecks {

    private TimeChecks() {}

    /**
     * Sevkiyat sınırdan uzun süredir yoldaysa {@code TIME_IN_TRANSIT_EXCEEDED} bulgusu; beklenen değer sınır, gerçek
     * değer geçen süredir (tam saat).
     */
    public static Optional<Finding> inTransit(Instant dispatchedAt, Instant now, TimeLimit limit) {
        Duration elapsed = Duration.between(dispatchedAt, now);
        if (elapsed.compareTo(Duration.ofHours(limit.inTransitMaxHours())) <= 0) {
            return Optional.empty();
        }
        return Optional.of(
                Finding.of(ComparisonRule.TIME_IN_TRANSIT_EXCEEDED, limit.inTransitMaxHours(), elapsed.toHours()));
    }
}
