package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecipeTest {

    private static final int THIRD = 3;

    private static RecipeLine line(int no, String kg) {
        return new RecipeLine(no, null, "iplik " + no, null, new BigDecimal(kg));
    }

    @Test
    @DisplayName("Reçete toplamı satır kiloları toplamıdır")
    void shouldSumLines() {
        assertThat(Recipe.totalKg(List.of(line(1, "19488.000"), line(2, "13492.000"), line(THIRD, "2036.000"))))
                .isEqualByComparingTo("35016.000");
    }

    @Test
    @DisplayName("Müdürün düzelttiği kilolar toplamı tartıya eşitse satırlara yazılır")
    void shouldApplyCorrectedWeights() {
        List<RecipeLine> corrected = Recipe.correct(
                List.of(line(1, "100.000"), line(2, "100.000")),
                List.of(
                        new Recipe.LineWeight(1, new BigDecimal("120.500")),
                        new Recipe.LineWeight(2, new BigDecimal("79.5"))),
                new BigDecimal("200.000"));

        assertThat(corrected.get(0).netKg()).isEqualByComparingTo("120.500");
        assertThat(Recipe.totalKg(corrected)).isEqualByComparingTo("200.000");
    }

    @Test
    @DisplayName("Düzeltilen toplam tartıdan farklıysa, satır eksikse ya da kilo geçersizse reddedilir")
    void shouldRejectInvalidCorrection() {
        List<RecipeLine> lines = List.of(line(1, "100.000"), line(2, "100.000"));
        BigDecimal weighed = new BigDecimal("200.000");
        Recipe.LineWeight one = new Recipe.LineWeight(1, new BigDecimal("100.000"));

        assertThatThrownBy(() -> Recipe.correct(
                        lines, List.of(one, new Recipe.LineWeight(2, new BigDecimal("99.999"))), weighed))
                .isInstanceOf(InvalidWeightDecisionException.class);
        assertThatThrownBy(() -> Recipe.correct(lines, List.of(one), weighed))
                .isInstanceOf(InvalidWeightDecisionException.class);
        assertThatThrownBy(() -> Recipe.correct(
                        lines, List.of(one, new Recipe.LineWeight(THIRD, new BigDecimal("100.000"))), weighed))
                .isInstanceOf(InvalidWeightDecisionException.class);
        assertThatThrownBy(() -> Recipe.correct(
                        lines,
                        List.of(
                                new Recipe.LineWeight(1, new BigDecimal("200.000")),
                                new Recipe.LineWeight(2, BigDecimal.ZERO)),
                        weighed))
                .isInstanceOf(InvalidWeightDecisionException.class);
    }

    @Test
    @DisplayName("Reçete satırı cinssiz, sıfır kg ya da dört ondalıklı kg ile kurulamaz")
    void shouldRejectInvalidLine() {
        assertThatThrownBy(() -> new RecipeLine(1, null, " ", null, BigDecimal.ONE))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new RecipeLine(1, null, "x", null, BigDecimal.ZERO))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new RecipeLine(1, null, "x", null, new BigDecimal("1.0001")))
                .isInstanceOf(InvalidNewShipmentException.class);
    }
}
