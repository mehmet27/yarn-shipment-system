package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * Otomatik karşılaştırma (ön analiz, Bölüm 9 ve 12.1). Miktar net kilo üzerinden ve tolerans bantlarıyla; çuval
 * sayısı, Unique Key listesi ve mühür sıfır toleransla; irsaliye alanları teslim kayıtlarıyla karşılaştırılır.
 */
public final class Comparison {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int PCT_SCALE = 2;
    private static final int RATIO_SCALE = 6;
    private static final String LIST_SEPARATOR = ",";

    private Comparison() {}

    public static ComparisonResult compare(ComparisonInput input, ToleranceBands bands) {
        List<Finding> findings = new ArrayList<>();
        Quantity quantity = compareQuantity(input, bands, findings);
        compareBags(input, findings);
        compareSeal(input, findings);
        compareDocument(input, findings);
        return new ComparisonResult(
                input.shipmentId(), quantity.band(), quantity.diffKg(), quantity.diffPct(), findings);
    }

    private static Quantity compareQuantity(ComparisonInput input, ToleranceBands bands, List<Finding> findings) {
        BigDecimal dispatch = input.dispatch().netKg();
        BigDecimal delivery = input.delivery().netKg();
        if (dispatch == null) {
            findings.add(Finding.of(ComparisonRule.QUANTITY_DISPATCH_MISSING, null, delivery));
            return new Quantity(Band.RED, null, null);
        }
        BigDecimal diffKg = dispatch.subtract(delivery);
        BigDecimal diffPct = percentage(diffKg, dispatch);
        Band band = bands.classify(diffPct);
        if (band != Band.GREEN) {
            Severity severity = band == Band.RED ? Severity.RED : Severity.YELLOW;
            findings.add(new Finding(
                    DiscrepancyType.QUANTITY,
                    severity,
                    "QUANTITY_TOLERANCE",
                    dispatch.toPlainString(),
                    delivery.toPlainString(),
                    diffKg,
                    diffPct));
        }
        return new Quantity(band, diffKg, diffPct);
    }

    /** Çıkış sıfırsa teslimde herhangi bir kilo sonsuz fark sayılır; iki taraf da sıfırsa fark yoktur. */
    static BigDecimal percentage(BigDecimal diffKg, BigDecimal dispatch) {
        if (dispatch.signum() == 0) {
            return diffKg.signum() == 0 ? BigDecimal.ZERO.setScale(PCT_SCALE) : HUNDRED.setScale(PCT_SCALE);
        }
        return diffKg.divide(dispatch, RATIO_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(PCT_SCALE, RoundingMode.HALF_UP);
    }

    private static void compareBags(ComparisonInput input, List<Finding> findings) {
        Integer dispatchBags = input.dispatch().bagCount();
        if (dispatchBags != null && dispatchBags != input.delivery().bagCount()) {
            findings.add(Finding.of(
                    ComparisonRule.BAG_COUNT_MISMATCH,
                    dispatchBags,
                    input.delivery().bagCount()));
        }
        Set<String> missing = new TreeSet<>(input.expectedKeys());
        missing.removeAll(input.scannedKeys());
        Set<String> extra = new TreeSet<>(input.scannedKeys());
        extra.removeAll(input.expectedKeys());
        if (!missing.isEmpty() || !extra.isEmpty()) {
            findings.add(Finding.of(
                    ComparisonRule.BAG_KEYS_MISMATCH,
                    "missing=" + String.join(LIST_SEPARATOR, missing),
                    "extra=" + String.join(LIST_SEPARATOR, extra)));
        }
    }

    private static void compareSeal(ComparisonInput input, List<Finding> findings) {
        DeliveryFigures delivery = input.delivery();
        if (delivery.sealCondition() != SealCondition.INTACT) {
            findings.add(Finding.of(ComparisonRule.SEAL_NOT_INTACT, SealCondition.INTACT, delivery.sealCondition()));
            return;
        }
        if (!Objects.equals(normalized(input.dispatch().sealNo()), normalized(delivery.sealNo()))) {
            findings.add(
                    Finding.of(ComparisonRule.SEAL_NO_MISMATCH, input.dispatch().sealNo(), delivery.sealNo()));
        }
    }

    /** İmzalı belge zorunludur (karar #5); girilen irsaliye alanları teslim kayıtlarıyla tutmalıdır. */
    private static void compareDocument(ComparisonInput input, List<Finding> findings) {
        DocumentFigures document = input.document();
        if (document.signedDocumentEvidenceId() == null) {
            findings.add(Finding.of(ComparisonRule.DOCUMENT_MISSING, null, null));
        }
        String documentNo = document.shipmentNo();
        if (documentNo != null
                && input.shipmentNo() != null
                && !Objects.equals(normalized(documentNo), normalized(input.shipmentNo()))) {
            findings.add(Finding.of(ComparisonRule.DOCUMENT_SHIPMENT_NO_MISMATCH, input.shipmentNo(), documentNo));
        }
        compareDocumentFigures(input, findings);
    }

    private static void compareDocumentFigures(ComparisonInput input, List<Finding> findings) {
        DocumentFigures document = input.document();
        BigDecimal documentKg = document.netKg();
        if (documentKg != null && documentKg.compareTo(input.delivery().netKg()) != 0) {
            findings.add(Finding.of(
                    ComparisonRule.DOCUMENT_NET_KG_MISMATCH,
                    input.delivery().netKg().toPlainString(),
                    documentKg.toPlainString()));
        }
        Integer documentBags = document.bagCount();
        if (documentBags != null && documentBags != input.delivery().bagCount()) {
            findings.add(Finding.of(
                    ComparisonRule.DOCUMENT_BAG_COUNT_MISMATCH, input.delivery().bagCount(), documentBags));
        }
    }

    private static @Nullable String normalized(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip().toUpperCase(Locale.ROOT);
        return stripped.isEmpty() ? null : stripped;
    }

    /** Miktar karşılaştırmasının ara sonucu. */
    private record Quantity(
            Band band,
            @Nullable BigDecimal diffKg,
            @Nullable BigDecimal diffPct) {}
}
