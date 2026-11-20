package com.yarnshipment.shipment.domain.fabric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kumaş ve fatura kilosu iplik cinslerine dağıtılırken hiçbir gram kaybolmaz ve paylar oranına uygundur. */
class YarnDistributionInvariantTest {

    private static final long SEED = 20261119L;
    private static final int CASES = 500;
    private static final int MAX_YARNS = 6;
    private static final int MAX_GRAMS = 50_000_000;
    private static final int GRAM_SCALE = 3;
    private static final BigDecimal ONE_GRAM = new BigDecimal("0.001");
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000b01");

    private static BigDecimal grams(long grams) {
        return BigDecimal.valueOf(grams, GRAM_SCALE);
    }

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
    @DisplayName(
            "Rastgele 500 reçetede payların toplamı kumaş kilosuna eşittir ve her pay tam oranından 1 gramdan az sapar")
    void shouldAlwaysSumToFabricKgAndStayWithinOneGramOfExactShare() {
        // given
        Random random = new Random(SEED);

        for (int i = 0; i < CASES; i++) {
            Map<String, BigDecimal> recipe = new LinkedHashMap<>();
            int yarns = 1 + random.nextInt(MAX_YARNS);
            for (int y = 0; y < yarns; y++) {
                recipe.put("İplik " + y, grams(1L + random.nextInt(MAX_GRAMS)));
            }
            BigDecimal fabricKg = grams(1L + random.nextInt(MAX_GRAMS));
            BigDecimal recipeTotal = recipe.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            // when
            List<YarnShare> shares = YarnDistribution.proportional(fabricKg, recipe);

            // then
            assertThat(total(shares)).as("toplam, %s kg / %s", fabricKg, recipe).isEqualByComparingTo(fabricKg);
            for (YarnShare share : shares) {
                BigDecimal exact = fabricKg.multiply(recipe.get(share.yarnType()))
                        .divide(recipeTotal, GRAM_SCALE + GRAM_SCALE, RoundingMode.HALF_UP);
                assertThat(share.netKg().subtract(exact).abs())
                        .as("%s payı, %s kg / %s", share.yarnType(), fabricKg, recipe)
                        .isLessThan(ONE_GRAM);
            }
        }
    }

    @Test
    @DisplayName("Tek gram en büyük paya gider; pay alamayan iplikler listede yer almaz")
    void shouldGiveSingleGramToLargestShare() {
        // when
        List<YarnShare> shares = YarnDistribution.proportional(new BigDecimal("0.001"), recipe("A", "300", "B", "700"));

        // then
        assertThat(shares).singleElement().satisfies(share -> {
            assertThat(share.yarnType()).isEqualTo("B");
            assertThat(share.netKg().toPlainString()).isEqualTo("0.001");
        });
    }

    @Test
    @DisplayName("Eşit paylarda artan gram reçete sırasındaki ilk ipliğe verilir; sonuç her çağrıda aynıdır")
    void shouldBreakTiesByRecipeOrder() {
        // when: 100 g üç eşit ipliğe -> 33 g + 1 g artan
        List<YarnShare> shares =
                YarnDistribution.proportional(new BigDecimal("0.100"), recipe("A", "5", "B", "5", "C", "5"));

        // then
        assertThat(shares)
                .extracting(share -> share.yarnType() + "=" + share.netKg().toPlainString())
                .containsExactly("A=0.034", "B=0.033", "C=0.033");
    }

    @Test
    @DisplayName("Reçetede kilosu sıfır olan iplik pay almaz; reçete toplamı sıfırsa dağıtım reddedilir")
    void shouldSkipZeroRecipeLinesAndRejectZeroTotal() {
        // when
        List<YarnShare> shares = YarnDistribution.proportional(new BigDecimal("10"), recipe("A", "0", "B", "40"));

        // then
        assertThat(shares).extracting(YarnShare::yarnType).containsExactly("B");
        assertThat(total(shares)).isEqualByComparingTo("10");
        assertThatThrownBy(() -> YarnDistribution.proportional(new BigDecimal("10"), recipe("A", "0", "B", "0")))
                .isInstanceOf(InvalidLedgerInputException.class);
    }

    @Test
    @DisplayName("Sıfır kilo dağıtılınca hiçbir pay çıkmaz")
    void shouldReturnNoSharesForZeroKg() {
        assertThat(YarnDistribution.proportional(BigDecimal.ZERO, recipe("A", "600", "B", "400")))
                .isEmpty();
    }

    @Test
    @DisplayName("Elle verilen paylarda aynı iplik farklı yazılsa da birleşir; sevkiyatta olmayan iplik reddedilir")
    void shouldMergeExplicitSharesByCanonicalName() {
        // given
        Map<String, BigDecimal> dispatched = recipe("Pamuk İplik 30/1", "600", "Polyester", "400");
        List<YarnShare> given = List.of(
                new YarnShare(" pamuk iplik 30/1 ", new BigDecimal("10.5")),
                new YarnShare("PAMUK İPLİK 30/1", new BigDecimal("4.5")),
                new YarnShare("polyester", new BigDecimal("5")));

        // when
        List<YarnShare> shares = YarnDistribution.explicit(given, dispatched, SHIPMENT);

        // then
        assertThat(shares)
                .extracting(share -> share.yarnType() + "=" + share.netKg().toPlainString())
                .containsExactly("Pamuk İplik 30/1=15.000", "Polyester=5.000");
        assertThatThrownBy(() -> YarnDistribution.explicit(
                        List.of(new YarnShare("Viskon", BigDecimal.ONE)), dispatched, SHIPMENT))
                .isInstanceOf(FabricYarnNotInShipmentException.class);
    }

    @Test
    @DisplayName("Sıfır ya da eksi kilolu iplik payı reddedilir")
    void shouldRejectNonPositiveShare() {
        assertThatThrownBy(() -> new YarnShare("A", BigDecimal.ZERO)).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> new YarnShare("A", new BigDecimal("-1")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> new YarnShare("A", new BigDecimal("0.0004")))
                .isInstanceOf(InvalidLedgerInputException.class);
    }
}
