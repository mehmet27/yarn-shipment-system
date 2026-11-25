package com.yarnshipment.shipment.domain.fabric;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FabricRecipeTest {

    private static FabricRecipe.Line line(String yarn, String pct) {
        return new FabricRecipe.Line(yarn, new BigDecimal(pct));
    }

    @Test
    @DisplayName("Oranları toplamı 100 olan reçete kabul edilir")
    void shouldAcceptHundredPercent() {
        assertThatCode(() ->
                        new FabricRecipe(UUID.randomUUID(), "Süprem", List.of(line("A", "70"), line("B", "30")), true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Oranları toplamı 100 olmayan reçete reddedilir")
    void shouldRejectOtherTotals() {
        assertThatThrownBy(() ->
                        new FabricRecipe(UUID.randomUUID(), "Süprem", List.of(line("A", "70"), line("B", "20")), true))
                .isInstanceOf(InvalidLedgerInputException.class);
    }

    @Test
    @DisplayName("Aynı iplik iki kez yazılamaz")
    void shouldRejectDuplicateYarn() {
        assertThatThrownBy(() ->
                        new FabricRecipe(UUID.randomUUID(), "Süprem", List.of(line("A", "50"), line("a", "50")), true))
                .isInstanceOf(InvalidLedgerInputException.class);
    }
}
