package com.yarnshipment.shipment.domain.fire;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Fire hesabının sınır durumları (analiz 8.1 ve 8.3): gram ve yüzde yuvarlaması, sıfır ve en yüksek fire oranı, dönen
 * iplik ile itirazın birlikte olduğu dosya ve parça parça fatura. Beklenen değerler formülden elle hesaplanmıştır.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class FireCalculationBoundaryTest {

    private static BigDecimal kg(String value) {
        return new BigDecimal(value);
    }

    /** Sıra: G, D, I, K, f. */
    private static FireCalculation calculate(String... values) {
        return FireCalculation.of(
                new Inputs(kg(values[0]), kg(values[1]), kg(values[2]), kg(values[3]), kg(values[4])));
    }

    /** G=1.000, I=100 (faturayla düşülecek), K=904, f=%4: T=900, B=864, K-I=804, eksik 60, faturalanacak 160. */
    private static FireCalculation claimAndShortage(String invoicedKg, String claimInvoicedKg) {
        return FireCalculation.of(new Inputs(
                kg("1000"),
                BigDecimal.ZERO,
                kg("100"),
                kg("904"),
                kg("0.04"),
                kg(invoicedKg),
                kg("100"),
                kg(claimInvoicedKg)));
    }

    @Test
    @DisplayName("Yarım gram yukarı yuvarlanır: 0,0005 kg eksik 1 gram faturalanacak kilo olur")
    void shouldRoundHalfGramShortageUp() {
        // when: G=100,010, f=%5 -> B=95,0095; K=95,009 -> E=0,0005
        FireCalculation result = calculate("100.010", "0", "0", "95.009", "0.05");

        // then
        assertThat(result.expectedFabricKg().toPlainString()).isEqualTo("95.010");
        assertThat(result.shortageKg().toPlainString()).isEqualTo("0.001");
        assertThat(result.faultyKg().toPlainString()).isEqualTo("0.001");
        assertThat(result.remainingKg().toPlainString()).isEqualTo("0.001");
        assertThat(result.hasFaultyKg()).isTrue();
    }

    @Test
    @DisplayName("Kumaş beklenenden yarım gram fazlaysa faturalanacak kilo çıkmaz")
    void shouldNotInvoiceWhenFabricIsHalfGramOverExpected() {
        // when: B=95,0095; K=95,010 -> E=-0,0005
        FireCalculation result = calculate("100.010", "0", "0", "95.010", "0.05");

        // then
        assertThat(result.faultyKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.remainingKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.hasFaultyKg()).isFalse();
    }

    @ParameterizedTest(name = "K = {0} kg -> yüzde {1}")
    @CsvSource({
        "959.950, 4.01",
        "959.951, 4.00",
        "960.000, 4.00",
        "1000.000, 0.00",
        "1000.050, -0.01",
        "1010.000, -1.00",
        "0.001, 100.00"
    })
    @DisplayName("Gerçekleşen fire yüzdesi 2 ondalığa yarım yukarı yuvarlanır; kumaş fazlaysa eksi çıkar")
    void shouldRoundFirePercentHalfUp(String fabricKg, String expectedPercent) {
        // when: T=1.000 kg
        FireCalculation result = calculate("1000", "0", "0", fabricKg, "0.04");

        // then
        assertThat(result.actualFirePercent().toPlainString()).isEqualTo(expectedPercent);
    }

    @Test
    @DisplayName("Fire oranı sıfırsa gönderilen ipliğin tamamı kumaş olarak beklenir")
    void shouldExpectAllYarnAsFabricWithZeroFireRate() {
        // when
        FireCalculation exact = calculate("500", "0", "0", "500", "0");
        FireCalculation oneGramShort = calculate("500", "0", "0", "499.999", "0");

        // then
        assertThat(exact.expectedFabricKg().toPlainString()).isEqualTo("500.000");
        assertThat(exact.hasFaultyKg()).isFalse();
        assertThat(exact.actualFirePercent().toPlainString()).isEqualTo("0.00");
        assertThat(oneGramShort.faultyKg().toPlainString()).isEqualTo("0.001");
    }

    @Test
    @DisplayName("En yüksek fire oranında (yüzde 20) 1.000 kg iplikten 800 kg kumaş beklenir")
    void shouldApplyMaximumFireRate() {
        // when
        FireCalculation exact = calculate("1000", "0", "0", "800", "0.20");
        FireCalculation oneGramShort = calculate("1000", "0", "0", "799.999", "0.20");

        // then
        assertThat(exact.expectedFabricKg().toPlainString()).isEqualTo("800.000");
        assertThat(exact.actualFirePercent().toPlainString()).isEqualTo("20.00");
        assertThat(exact.hasFaultyKg()).isFalse();
        assertThat(oneGramShort.faultyKg().toPlainString()).isEqualTo("0.001");
    }

    @Test
    @DisplayName("Hiç kumaş gelmediyse beklenen kumaşın tamamı faturalanacak kilodur")
    void shouldInvoiceWholeExpectedFabricWhenNothingArrived() {
        // when
        FireCalculation result = calculate("1000", "0", "0", "0", "0.04");

        // then
        assertThat(result.faultyKg().toPlainString()).isEqualTo("960.000");
        assertThat(result.actualFirePercent().toPlainString()).isEqualTo("100.00");
    }

    @Test
    @DisplayName("Dönen iplik ve haklı itiraz birlikte tabandan düşer: 1.000 - 100 - 50 = 850 kg")
    void shouldSubtractReturnAndClaimTogether() {
        // when: T=850, B=816, K-I=866-50=816
        FireCalculation result = calculate("1000", "100", "50", "866", "0.04");

        // then
        assertThat(result.baseKg().toPlainString()).isEqualTo("850.000");
        assertThat(result.expectedFabricKg().toPlainString()).isEqualTo("816.000");
        assertThat(result.shortageKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.actualFirePercent().toPlainString()).isEqualTo("4.00");
    }

    @Test
    @DisplayName("Gönderilen ipliğin tamamı döndüyse hesap sıfıra bölmez; faturalanacak kilo yoktur")
    void shouldHandleEverythingReturned() {
        // when
        FireCalculation result = calculate("750.250", "750.250", "0", "0", "0.04");

        // then
        assertThat(result.baseKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.expectedFabricKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.actualFirePercent().toPlainString()).isEqualTo("0.00");
        assertThat(result.hasFaultyKg()).isFalse();
    }

    @Test
    @DisplayName("Parça parça faturada kalan kilo her adımda faturalanan kadar azalır ve sıfırın altına inmez")
    void shouldReduceRemainingByEachPartialInvoice() {
        // given: G=1.000, f=%4, K=900 -> eksik 60 kg; faturalar 10,5 + 20,25 + 29,25 = 60
        String[][] steps = {{"0", "60.000"}, {"10.5", "49.500"}, {"30.75", "29.250"}, {"60", "0.000"}};

        for (String[] step : steps) {
            // when
            FireCalculation result = FireCalculation.of(
                    new Inputs(kg("1000"), BigDecimal.ZERO, BigDecimal.ZERO, kg("900"), kg("0.04"), kg(step[0])));

            // then: eksik kilo değişmez, kalan = eksik - faturalanan
            assertThat(result.faultyKg().toPlainString()).isEqualTo("60.000");
            assertThat(result.remainingKg().toPlainString()).isEqualTo(step[1]);
            assertThat(result.remainingKg().add(result.invoicedKg())).isEqualByComparingTo("60");
        }
    }

    @Test
    @DisplayName("İtiraz ve eksik kumaş birlikteyken fatura önce itirazı, sonra eksiği karşılar; toplam kaybolmaz")
    void shouldSplitInvoiceBetweenClaimAndShortage() {
        // when
        FireCalculation open = claimAndShortage("0", "0");
        FireCalculation claimPartly = claimAndShortage("70", "70");
        FireCalculation claimDone = claimAndShortage("130", "100");
        FireCalculation done = claimAndShortage("160", "100");

        // then: faturalanacak 60 (eksik) + 100 (itiraz) = 160
        assertThat(open.baseKg().toPlainString()).isEqualTo("900.000");
        assertThat(open.faultyKg().toPlainString()).isEqualTo("60.000");
        assertThat(open.remainingKg().toPlainString()).isEqualTo("160.000");
        assertThat(open.actualFirePercent().toPlainString()).isEqualTo("10.67");
        // 70 kg itirazı karşıladı: itirazdan 30, eksikten 60 kaldı; fire yüzdesi değişmez
        assertThat(claimPartly.remainingKg().toPlainString()).isEqualTo("90.000");
        assertThat(claimPartly.claimRemainingKg().toPlainString()).isEqualTo("30.000");
        assertThat(claimPartly.actualFirePercent().toPlainString()).isEqualTo("10.67");
        // 130 kg: itiraz bitti, 30 kg eksiği karşıladı -> fire (900 - 804 - 30) / 900 = %7,33
        assertThat(claimDone.remainingKg().toPlainString()).isEqualTo("30.000");
        assertThat(claimDone.shortageInvoicedKg()).isEqualByComparingTo("30");
        assertThat(claimDone.actualFirePercent().toPlainString()).isEqualTo("7.33");
        // tamamı faturalandı: fire sevkiyatın oranına iner
        assertThat(done.remainingKg().toPlainString()).isEqualTo("0.000");
        assertThat(done.hasFaultyKg()).isFalse();
        assertThat(done.actualFirePercent().toPlainString()).isEqualTo("4.00");
    }

    @Test
    @DisplayName("Fatura kesildikten sonra gelen kumaş eksiği faturalananın altına indirirse kalan eksiye düşmez")
    void shouldNotGoNegativeWhenFabricArrivesAfterInvoice() {
        // when: eksik 60 kg faturalandı, sonra kumaş 940 kg'a çıktı -> eksik 20 kg
        FireCalculation result = FireCalculation.of(
                new Inputs(kg("1000"), BigDecimal.ZERO, BigDecimal.ZERO, kg("940"), kg("0.04"), kg("60")));

        // then
        assertThat(result.faultyKg().toPlainString()).isEqualTo("20.000");
        assertThat(result.remainingKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.hasFaultyKg()).isFalse();
    }
}
