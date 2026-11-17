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
    @DisplayName("Reçete toplamı tartılan net'ten en çok tolerans kadar farklıysa tartıyı tutar")
    void shouldFitWeighingWithinTolerance() {
        List<RecipeLine> lines = List.of(line(1, "1000.000"), line(2, "980.000"));
        BigDecimal tolerance = new BigDecimal("5.000");

        assertThat(Recipe.fitsWeighing(lines, new BigDecimal("1980.000"), tolerance))
                .isTrue();
        assertThat(Recipe.fitsWeighing(lines, new BigDecimal("1985.000"), tolerance))
                .isTrue();
        assertThat(Recipe.fitsWeighing(lines, new BigDecimal("1975.000"), tolerance))
                .isTrue();
        assertThat(Recipe.fitsWeighing(lines, new BigDecimal("1985.001"), tolerance))
                .isFalse();
        assertThat(Recipe.fitsWeighing(lines, new BigDecimal("1974.999"), tolerance))
                .isFalse();
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
