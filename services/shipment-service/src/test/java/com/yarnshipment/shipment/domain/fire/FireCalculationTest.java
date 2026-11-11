package com.yarnshipment.shipment.domain.fire;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Analiz Bölüm 8.2 örnekleri ve Kırıkhan kabul testi. */
@SuppressWarnings("checkstyle:MagicNumber")
class FireCalculationTest {

    /** Sıra: G, D, I, K, f. */
    private static FireCalculation calculate(String... values) {
        return FireCalculation.of(new Inputs(
                new BigDecimal(values[0]),
                new BigDecimal(values[1]),
                new BigDecimal(values[2]),
                new BigDecimal(values[3]),
                new BigDecimal(values[4])));
    }

    @ParameterizedTest(name = "G, D, I, f, K, B, H = {0}")
    @CsvSource(
            delimiter = ';',
            value = {
                "1000, 0, 0, 0.04, 960, 960.000, 0.000",
                "1000, 0, 0, 0.04, 900, 960.000, 60.000",
                "1000, 100, 0, 0.04, 864, 864.000, 0.000",
                "1000, 0, 100, 0.04, 964, 864.000, 0.000",
                "1000, 0, 0, 0.05, 940, 950.000, 10.000",
                "34032, 0, 0, 0.04, 32660, 32670.720, 10.720"
            })
    @DisplayName("Bölüm 8.2 örnekleri beklenen kumaşı ve hatalı kiloyu verir")
    void shouldMatchSectionExamples(String line) {
        // given
        String[] row = line.split(",\\s*", -1);

        // when
        FireCalculation result = calculate(row[0], row[1], row[2], row[4], row[3]);

        // then
        assertThat(result.expectedFabricKg()).isEqualByComparingTo(row[5]);
        assertThat(result.faultyKg()).isEqualByComparingTo(row[6]);
    }

    @Test
    @DisplayName("Kırıkhan: 34.032 kg gidip 32.660 kg gelince gerçekleşen fire yüzde 4,03, hatalı kg 10,720 çıkar")
    void shouldReproduceKirikhanFire() {
        // when
        FireCalculation result = calculate("34032", "0", "0", "32660", "0.04");

        // then
        assertThat(result.actualFirePercent()).isEqualByComparingTo("4.03");
        assertThat(result.faultyKg().toPlainString()).isEqualTo("10.720");
        assertThat(result.expectedFabricKg().toPlainString()).isEqualTo("32670.720");
        assertThat(result.baseKg().toPlainString()).isEqualTo("34032.000");
        assertThat(result.hasFaultyKg()).isTrue();
    }

    @Test
    @DisplayName("Haklı itiraz kilosu hem tabandan hem gelen kumaştan düşer")
    void shouldSubtractClaimFromBaseAndFabric() {
        // when
        FireCalculation result = calculate("1000", "0", "100", "964", "0.04");

        // then
        assertThat(result.baseKg()).isEqualByComparingTo("900");
        assertThat(result.shortageKg()).isEqualByComparingTo("0");
        assertThat(result.faultyKg()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Kumaş beklenenden fazlaysa eksik eksi çıkar, hatalı kg sıfır kalır")
    void shouldNotReportNegativeFaultyKg() {
        // when
        FireCalculation result = calculate("1000", "0", "0", "980", "0.04");

        // then
        assertThat(result.shortageKg()).isEqualByComparingTo("-20");
        assertThat(result.faultyKg()).isEqualByComparingTo("0");
        assertThat(result.hasFaultyKg()).isFalse();
    }

    @Test
    @DisplayName("Taban sıfırsa gerçekleşen fire sıfır yüzdedir, sıfıra bölünmez")
    void shouldHandleZeroBase() {
        // when
        FireCalculation result = calculate("500", "500", "0", "0", "0.04");

        // then
        assertThat(result.actualFirePercent()).isEqualByComparingTo("0");
        assertThat(result.faultyKg()).isEqualByComparingTo("0");
    }
}
