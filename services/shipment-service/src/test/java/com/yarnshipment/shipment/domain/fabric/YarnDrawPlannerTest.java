package com.yarnshipment.shipment.domain.fabric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner.Plan;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// İstisna: SIZE-04, test satırı alanları tek tek okunur yazılır
@SuppressWarnings({"checkstyle:ParameterNumber", "checkstyle:MagicNumber"})
class YarnDrawPlannerTest {

    private static final UUID ENTRY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final UUID OLD = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e2");
    private static final UUID MID = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e3");
    private static final UUID LOT_A = UUID.fromString("0192f0a0-0000-7000-8000-0000000000f1");
    private static final UUID LOT_B = UUID.fromString("0192f0a0-0000-7000-8000-0000000000f2");

    private static YarnStockRow row(UUID shipment, String since, String yarn, @Nullable UUID lot, String kg) {
        return new YarnStockRow(
                shipment,
                "SV-" + shipment.toString().substring(35),
                Instant.parse(since),
                yarn,
                yarn,
                lot,
                null,
                null,
                new BigDecimal(kg));
    }

    private static YarnRatio ratio(String yarn, @Nullable UUID lot, String pct) {
        return new YarnRatio(yarn, lot, new BigDecimal(pct));
    }

    @Test
    @DisplayName("Kumaş kilosu oranlarla bölünür; gram artığı toplamı bozmaz")
    void shouldSplitKgByRatioWithoutLosingGrams() {
        List<YarnStockRow> stock = List.of(
                row(ENTRY, "2026-11-01T00:00:00Z", "A", null, "1000"),
                row(ENTRY, "2026-11-01T00:00:00Z", "B", null, "1000"),
                row(ENTRY, "2026-11-01T00:00:00Z", "C", null, "1000"));

        Plan plan = YarnDrawPlanner.plan(
                ENTRY,
                new BigDecimal("100.000"),
                List.of(ratio("A", null, "33.33"), ratio("B", null, "33.33"), ratio("C", null, "33.34")),
                stock);

        assertThat(plan.isFeasible()).isTrue();
        assertThat(plan.yarns().stream().map(FabricYarn::netKg).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.000");
        assertThat(plan.yarns().get(2).netKg()).isEqualByComparingTo("33.340");
    }

    @Test
    @DisplayName("Önce girildiği sevkiyat, sonra en eski sevkiyat; en yeniye yalnız gerekirse inilir")
    void shouldDrawEntryThenOldest() {
        List<YarnStockRow> stock = List.of(
                row(MID, "2026-11-02T00:00:00Z", "A", null, "50"),
                row(OLD, "2026-11-01T00:00:00Z", "A", null, "30"),
                row(ENTRY, "2026-11-03T00:00:00Z", "A", null, "20"));

        Plan plan = YarnDrawPlanner.plan(ENTRY, new BigDecimal("90"), List.of(ratio("A", null, "100")), stock);

        List<YarnDraw> draws = plan.yarns().getFirst().draws();
        assertThat(draws).extracting(YarnDraw::shipmentId).containsExactly(ENTRY, OLD, MID);
        assertThat(draws)
                .extracting(YarnDraw::netKg)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("20"), new BigDecimal("30"), new BigDecimal("40"));
    }

    @Test
    @DisplayName("Aynı cinsin farklı partileri ayrı satırdır: oran partiye yazılır ve yalnız o partiden düşer")
    void shouldTreatLotsAsSeparateYarns() {
        List<YarnStockRow> stock = new ArrayList<>(List.of(
                row(ENTRY, "2026-11-01T00:00:00Z", "A", LOT_A, "40"),
                row(ENTRY, "2026-11-01T00:00:00Z", "A", LOT_B, "100")));

        Plan plan = YarnDrawPlanner.plan(
                ENTRY, new BigDecimal("100"), List.of(ratio("A", LOT_A, "50"), ratio("A", LOT_B, "50")), stock);

        assertThat(plan.isFeasible()).isFalse();
        assertThat(plan.shortages()).singleElement().satisfies(shortage -> {
            assertThat(shortage.lotId()).isEqualTo(LOT_A);
            assertThat(shortage.missingKg()).isEqualByComparingTo("10");
        });
        assertThat(plan.yarns().get(1).draws())
                .singleElement()
                .satisfies(draw -> assertThat(draw.lotId()).isEqualTo(LOT_B));
    }

    @Test
    @DisplayName("Aynı satırdan iki ipliğe aynı anda düşülemez: kalan kilo planda paylaşılır")
    void shouldNotDrawSameStockTwice() {
        List<YarnStockRow> stock = List.of(row(ENTRY, "2026-11-01T00:00:00Z", "A", null, "60"));

        Plan plan = YarnDrawPlanner.plan(
                ENTRY, new BigDecimal("100"), List.of(ratio("A", null, "50"), ratio("a ", LOT_A, "50")), stock);

        assertThat(plan.shortages()).hasSize(1);
        assertThat(plan.yarns().getFirst().draws()).hasSize(1);
    }

    @Test
    @DisplayName("Oran toplamı 100 değilse, boşsa ya da aynı iplik tekrar yazılmışsa reddedilir; oran iki ondalıktır")
    void shouldValidateRatios() {
        List<YarnStockRow> stock = List.of(row(ENTRY, "2026-11-01T00:00:00Z", "A", null, "60"));

        assertThatThrownBy(() -> YarnDrawPlanner.plan(ENTRY, BigDecimal.TEN, List.of(ratio("A", null, "99.99")), stock))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> YarnDrawPlanner.plan(ENTRY, BigDecimal.TEN, List.of(), stock))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> ratio("A", null, "33.333")).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> ratio("A", null, "0")).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> ratio("A", null, "100.01")).isInstanceOf(InvalidLedgerInputException.class);
    }
}
