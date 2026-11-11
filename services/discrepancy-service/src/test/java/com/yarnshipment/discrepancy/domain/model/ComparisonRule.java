package com.yarnshipment.discrepancy.domain.model;

/** Test verisi için kural kodları; adı {@code discrepancy.rule_code} olarak saklanır. */
public enum ComparisonRule {
    QUALITY_CLAIM(DiscrepancyType.QUALITY, Severity.RED),
    QUALITY_CLAIM_MINOR(DiscrepancyType.QUALITY, Severity.YELLOW);

    private final DiscrepancyType type;
    private final Severity severity;

    ComparisonRule(DiscrepancyType type, Severity severity) {
        this.type = type;
        this.severity = severity;
    }

    public DiscrepancyType type() {
        return type;
    }

    public Severity severity() {
        return severity;
    }
}
