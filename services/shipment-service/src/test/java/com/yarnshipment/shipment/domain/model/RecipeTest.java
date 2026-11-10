package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecipeTest {

    private static RecipeLine line(int no, String kg) {
        return new RecipeLine(no, null, "iplik " + no, null, new BigDecimal(kg));
    }

    @Test
    @DisplayName("Reçete toplamı satır kiloları toplamıdır")
    void shouldSumLines() {
        assertThat(Recipe.totalKg(List.of(line(1, "19488.000"), line(2, "13492.000"), line(3, "2036.000"))))
                .isEqualByComparingTo("35016.000");
    }

    @Test
    @DisplayName("Satırlar tartıya orantılı ölçeklenir ve toplam tam olarak tartıya eşit olur")
    void shouldScaleLinesAndKeepExactTotal() {
        List<RecipeLine> scaled = Recipe.scaleTo(
                List.of(line(1, "100.000"), line(2, "100.000"), line(3, "100.000")), new BigDecimal("200.000"));

        assertThat(Recipe.totalKg(scaled)).isEqualByComparingTo("200.000");
        assertThat(scaled)
                .extracting(RecipeLine::netKg)
                .allSatisfy(kg -> assertThat(kg).isPositive());
        assertThat(scaled.get(0).netKg()).isEqualByComparingTo("66.667");
        assertThat(scaled.get(2).netKg()).isEqualByComparingTo("66.666");
    }

    @Test
    @DisplayName("Ölçekleme bir satırı sıfıra indirecekse reddedilir")
    void shouldRejectScalingToZeroLine() {
        assertThatThrownBy(() -> Recipe.scaleTo(List.of(line(1, "0.001"), line(2, "100.000")), new BigDecimal("1.000")))
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
