package com.yarnshipment.discrepancy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComparisonTest {

    private static final ToleranceBands BANDS = ToleranceBands.REPORT_DEFAULT;

    @Test
    @DisplayName("Her alan tutuyorsa uyuşmazlık açılmaz; miktar yeşil banttadır")
    void shouldBeCleanWhenEverythingMatches() {
        ComparisonResult result = Comparison.compare(TestComparisons.clean(), BANDS);

        assertThat(result.clean()).isTrue();
        assertThat(result.quantityBand()).isEqualTo(Band.GREEN);
        assertThat(result.diffKg()).isEqualByComparingTo("0");
        assertThat(result.highestSeverity()).isEmpty();
    }

    @Test
    @DisplayName("%1 eksik teslim sarı, %2 eksik teslim kırmızı miktar uyuşmazlığı açar; fark kilo ve yüzde taşınır")
    void shouldClassifyQuantityByBand() {
        ComparisonResult yellow = Comparison.compare(withDeliveryKg("1980.000"), BANDS);
        ComparisonResult red = Comparison.compare(withDeliveryKg("1960.000"), BANDS);

        Finding finding = yellow.findings().getFirst();
        assertThat(finding.ruleCode()).isEqualTo("QUANTITY_TOLERANCE");
        assertThat(finding.severity()).isEqualTo(Severity.YELLOW);
        assertThat(finding.diffKg()).isEqualByComparingTo("20");
        assertThat(finding.diffPct()).isEqualByComparingTo("1.00");
        assertThat(red.quantityBand()).isEqualTo(Band.RED);
        assertThat(red.highestSeverity()).contains(Severity.RED);
    }

    @Test
    @DisplayName("Fazla teslim de mutlak farkla değerlendirilir; bant sınırı yeşile dahildir")
    void shouldUseAbsoluteDifference() {
        ComparisonResult over = Comparison.compare(withDeliveryKg("2040.000"), BANDS);
        ComparisonResult boundary = Comparison.compare(withDeliveryKg("1990.000"), BANDS);

        assertThat(over.quantityBand()).isEqualTo(Band.RED);
        assertThat(over.diffPct()).isEqualByComparingTo("-2.00");
        assertThat(boundary.quantityBand()).isEqualTo(Band.GREEN);
        assertThat(boundary.clean()).isTrue();
    }

    @Test
    @DisplayName("Çıkış tartısı yoksa miktar karşılaştırılamaz ve kırmızı uyuşmazlık açılır")
    void shouldFlagMissingDispatchWeighing() {
        ComparisonInput input = TestComparisons.input(
                new DispatchFigures(null, TestComparisons.BAGS, TestComparisons.SEAL),
                TestComparisons.delivery(TestComparisons.DISPATCH_KG),
                TestComparisons.KEYS,
                TestComparisons.document());

        ComparisonResult result = Comparison.compare(input, BANDS);

        assertThat(rules(result)).containsExactly("QUANTITY_DISPATCH_MISSING");
        assertThat(result.quantityBand()).isEqualTo(Band.RED);
        assertThat(result.diffPct()).isNull();
    }

    @Test
    @DisplayName("Çuval sayısı ve Unique Key listesi sıfır toleranslıdır; eksik ve fazla anahtarlar yazılır")
    void shouldFlagBagMismatches() {
        ComparisonInput input = TestComparisons.input(
                TestComparisons.dispatch(),
                new DeliveryFigures(TestComparisons.DISPATCH_KG, 1, TestComparisons.SEAL, SealCondition.INTACT),
                List.of("UK-0001", "UK-9999"),
                new DocumentFigures(null, null, null, TestComparisons.EVIDENCE));

        ComparisonResult result = Comparison.compare(input, BANDS);

        assertThat(rules(result)).containsExactly("BAG_COUNT_MISMATCH", "BAG_KEYS_MISMATCH");
        Finding keys = result.findings().get(1);
        assertThat(keys.expectedValue()).isEqualTo("missing=UK-0002");
        assertThat(keys.actualValue()).isEqualTo("extra=UK-9999");
    }

    @Test
    @DisplayName("Hasarlı mühür kırmızıdır; sağlam mühürde numara farkı açılır, harf büyüklüğü ve boşluk yok sayılır")
    void shouldFlagSealProblems() {
        ComparisonInput damaged = TestComparisons.input(
                TestComparisons.dispatch(),
                new DeliveryFigures(TestComparisons.DISPATCH_KG, TestComparisons.BAGS, "M-0002", SealCondition.DAMAGED),
                TestComparisons.KEYS,
                TestComparisons.document());
        ComparisonInput otherNo = withDeliverySeal("M-0002");
        ComparisonInput sameNo = withDeliverySeal(" m-0001 ");

        assertThat(rules(Comparison.compare(damaged, BANDS))).containsExactly("SEAL_NOT_INTACT");
        assertThat(rules(Comparison.compare(otherNo, BANDS))).containsExactly("SEAL_NO_MISMATCH");
        assertThat(Comparison.compare(sameNo, BANDS).clean()).isTrue();
    }

    @Test
    @DisplayName("İmzalı belge yoksa sarı uyuşmazlık açılır; girilen irsaliye alanları teslimle tutmalıdır")
    void shouldFlagDocumentProblems() {
        ComparisonInput missing = withDocument(DocumentFigures.NONE);
        ComparisonInput mismatched =
                withDocument(new DocumentFigures("SV-4", new BigDecimal("1999.000"), TestComparisons.BAGS + 1, null));

        ComparisonResult result = Comparison.compare(mismatched, BANDS);

        assertThat(rules(Comparison.compare(missing, BANDS))).containsExactly("DOCUMENT_MISSING");
        assertThat(rules(result))
                .containsExactly(
                        "DOCUMENT_MISSING",
                        "DOCUMENT_SHIPMENT_NO_MISMATCH",
                        "DOCUMENT_NET_KG_MISMATCH",
                        "DOCUMENT_BAG_COUNT_MISMATCH");
        assertThat(result.highestSeverity()).contains(Severity.YELLOW);
        assertThat(result.findings()).allMatch(finding -> finding.type() == DiscrepancyType.DOCUMENT);
    }

    /** İrsaliye teslimle aynı kiloyu taşır; yalnızca çıkış ile teslim arasındaki fark ölçülür. */
    private static ComparisonInput withDeliveryKg(String netKg) {
        BigDecimal kg = new BigDecimal(netKg);
        return TestComparisons.input(
                TestComparisons.dispatch(),
                TestComparisons.delivery(kg),
                TestComparisons.KEYS,
                new DocumentFigures(TestComparisons.SHIPMENT_NO, kg, TestComparisons.BAGS, TestComparisons.EVIDENCE));
    }

    private static ComparisonInput withDeliverySeal(String sealNo) {
        return TestComparisons.input(
                TestComparisons.dispatch(),
                new DeliveryFigures(TestComparisons.DISPATCH_KG, TestComparisons.BAGS, sealNo, SealCondition.INTACT),
                TestComparisons.KEYS,
                TestComparisons.document());
    }

    private static ComparisonInput withDocument(DocumentFigures document) {
        return TestComparisons.input(
                TestComparisons.dispatch(),
                TestComparisons.delivery(TestComparisons.DISPATCH_KG),
                TestComparisons.KEYS,
                document);
    }

    private static List<String> rules(ComparisonResult result) {
        return result.findings().stream().map(Finding::ruleCode).toList();
    }
}
