package com.yarnshipment.discrepancy.domain.model;

/**
 * Sabit öncelikli karşılaştırma kuralları; adı {@code discrepancy.rule_code} olarak saklanır. Miktar toleransı
 * önceliği banda göre değiştiği için burada değildir.
 */
public enum ComparisonRule {
    QUANTITY_DISPATCH_MISSING(DiscrepancyType.QUANTITY, Severity.RED),
    BAG_COUNT_MISMATCH(DiscrepancyType.BAG, Severity.RED),
    BAG_KEYS_MISMATCH(DiscrepancyType.BAG, Severity.RED),
    SEAL_NOT_INTACT(DiscrepancyType.SEAL, Severity.RED),
    SEAL_NO_MISMATCH(DiscrepancyType.SEAL, Severity.RED),
    DOCUMENT_MISSING(DiscrepancyType.DOCUMENT, Severity.YELLOW),
    DOCUMENT_SHIPMENT_NO_MISMATCH(DiscrepancyType.DOCUMENT, Severity.YELLOW),
    DOCUMENT_NET_KG_MISMATCH(DiscrepancyType.DOCUMENT, Severity.YELLOW),
    DOCUMENT_BAG_COUNT_MISMATCH(DiscrepancyType.DOCUMENT, Severity.YELLOW);

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
