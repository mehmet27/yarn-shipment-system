package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Karşılaştırmanın bulduğu bir uyuşmazlık; kaydedilince {@code discrepancy} satırı olur.
 *
 * @param type uyuşmazlık türü
 * @param severity öncelik
 * @param ruleCode tetikleyen kural, ör. {@code QUANTITY_TOLERANCE}
 * @param expectedValue çıkış tarafındaki değer
 * @param actualValue teslim tarafındaki değer
 * @param diffKg miktar uyuşmazlığında ve fiş kilo kontrollerinde net fark
 * @param diffPct miktar uyuşmazlığında ve fiş kilo kontrollerinde net fark yüzdesi
 */
public record Finding(
        DiscrepancyType type,
        Severity severity,
        String ruleCode,
        @Nullable String expectedValue,
        @Nullable String actualValue,
        @Nullable BigDecimal diffKg,
        @Nullable BigDecimal diffPct) {

    public Finding {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(ruleCode, "ruleCode");
    }

    static Finding of(ComparisonRule rule, @Nullable Object expected, @Nullable Object actual) {
        return new Finding(rule.type(), rule.severity(), rule.name(), text(expected), text(actual), null, null);
    }

    private static @Nullable String text(@Nullable Object value) {
        return value == null ? null : value.toString();
    }
}
