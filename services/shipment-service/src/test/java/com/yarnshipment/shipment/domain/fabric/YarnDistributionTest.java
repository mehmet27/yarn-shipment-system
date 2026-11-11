package com.yarnshipment.shipment.domain.fabric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class YarnDistributionTest {

    private static Map<String, BigDecimal> recipe(String... pairs) {
        Map<String, BigDecimal> recipe = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            recipe.put(pairs[i], new BigDecimal(pairs[i + 1]));
        }
        return recipe;
    }

    private static BigDecimal total(List<YarnShare> shares) {
        return shares.stream().map(YarnShare::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("Kumaş reçete oranıyla dağıtılır")
    void shouldDistributeByRecipeRatio() {
        // when
        List<YarnShare> shares = YarnDistribution.proportional(new BigDecimal("960"), recipe("A", "600", "B", "400"));

        // then
        assertThat(shares).extracting(YarnShare::yarnType).containsExactly("A", "B");
        assertThat(shares.get(0).netKg()).isEqualByComparingTo("576");
        assertThat(shares.get(1).netKg()).isEqualByComparingTo("384");
    }

    @Test
    @DisplayName("Bölünemeyen gramlar dağıtılır; toplam her zaman kumaş kilosuna eşittir")
    void shouldKeepTotalEqualToFabricKg() {
        // when
        List<YarnShare> shares =
                YarnDistribution.proportional(new BigDecimal("100.000"), recipe("A", "1", "B", "1", "C", "1"));

        // then
        assertThat(total(shares)).isEqualByComparingTo("100.000");
        assertThat(shares)
                .extracting(share -> share.netKg().toPlainString())
                .containsExactlyInAnyOrder("33.334", "33.333", "33.333");
    }

    @Test
    @DisplayName("Tek iplik cinsi kumaşın tamamını alır")
    void shouldGiveAllToSingleYarn() {
        // when
        List<YarnShare> shares = YarnDistribution.proportional(new BigDecimal("32660"), recipe("(reçetesiz)", "34032"));

        // then
        assertThat(shares)
                .singleElement()
                .satisfies(share -> assertThat(share.netKg()).isEqualByComparingTo("32660"));
    }

    @Test
    @DisplayName("Reçete boşsa dağıtım reddedilir")
    void shouldRejectEmptyRecipe() {
        assertThatThrownBy(() -> YarnDistribution.proportional(new BigDecimal("10"), Map.of()))
                .isInstanceOf(InvalidLedgerInputException.class);
    }
}
