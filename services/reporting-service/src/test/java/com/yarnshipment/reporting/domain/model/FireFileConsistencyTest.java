package com.yarnshipment.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Rapordaki fire dosyası, sevkiyat ekranındaki fire dosyasıyla aynı sayıları vermelidir. Buradaki girdiler ve elle
 * hesaplanmış beklenen değerler shipment-service'teki {@code FireCalculationBoundaryTest} ile aynıdır; iki ekranın
 * birbirinden sapması bu iki testten birini kırar.
 */
@SuppressWarnings("checkstyle:MagicNumber")
class FireFileConsistencyTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000e01");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000e02");

    /** Sıra: G, D, K, f, I, eksik için faturalanan, itirazdan faturalanacak kalan. */
    private static FireFile file(String... values) {
        return new FireFile(
                SHIPMENT,
                "SV-1",
                SUBCONTRACTOR,
                "Örnek Tekstil",
                new BigDecimal(values[0]),
                new BigDecimal(values[1]),
                new BigDecimal(values[2]),
                new BigDecimal(values[3]),
                "TO_INVOICE",
                null,
                null,
                new BigDecimal(values[4]),
                new BigDecimal(values[5]),
                new BigDecimal(values[6]));
    }

    @ParameterizedTest(name = "K = {0} kg -> fire {1}")
    @CsvSource({
        "959.950, 0.0401",
        "959.951, 0.0400",
        "960.000, 0.0400",
        "1000.000, 0.0000",
        "1000.050, -0.0001",
        "1010.000, -0.0100",
        "0.001, 1.0000"
    })
    @DisplayName("Gerçekleşen fire sevkiyat ekranındaki yüzdeyle aynı yuvarlanır; kumaş fazlaysa eksi çıkar")
    void shouldRoundFireRateLikeShipmentScreen(String fabricKg, String expectedRate) {
        // when: T=1.000 kg
        FireFile result = file("1000", "0", fabricKg, "0.04", "0", "0", "0");

        // then
        assertThat(result.actualFireRate().toPlainString()).isEqualTo(expectedRate);
    }

    @Test
    @DisplayName("Yarım gram eksik 1 gram faturalanacak kilo olur; yarım gram fazla kumaşta faturalanacak çıkmaz")
    void shouldRoundHalfGramShortageUp() {
        // when: G=100,010, f=%5 -> B=95,0095
        FireFile halfGramShort = file("100.010", "0", "95.009", "0.05", "0", "0", "0");
        FireFile halfGramOver = file("100.010", "0", "95.010", "0.05", "0", "0", "0");

        // then
        assertThat(halfGramShort.expectedFabricKg().toPlainString()).isEqualTo("95.010");
        assertThat(halfGramShort.toInvoiceKg().toPlainString()).isEqualTo("0.001");
        assertThat(halfGramShort.exceedsLimit()).isTrue();
        assertThat(halfGramOver.toInvoiceKg().toPlainString()).isEqualTo("0.000");
        assertThat(halfGramOver.exceedsLimit()).isFalse();
    }

    @Test
    @DisplayName("Dönen iplik ve haklı itiraz birlikte: taban 850 kg, beklenen 816 kg, fire yüzde 4")
    void shouldSubtractReturnAndClaimTogether() {
        // when: G=1.000, D=100, I=50, K=866
        FireFile result = file("1000", "100", "866", "0.04", "50", "0", "0");

        // then
        assertThat(result.baseKg()).isEqualByComparingTo("850");
        assertThat(result.expectedFabricKg().toPlainString()).isEqualTo("816.000");
        assertThat(result.shortageKg().toPlainString()).isEqualTo("0.000");
        assertThat(result.actualFireRate().toPlainString()).isEqualTo("0.0400");
    }

    @Test
    @DisplayName(
            "İtiraz ve eksik kumaş birlikteyken faturalanacak kilo ve fire her faturada sevkiyat ekranıyla aynıdır")
    void shouldMatchShipmentScreenThroughPartialInvoices() {
        // given: G=1.000, I=100, K=904, f=%4 -> T=900, B=864, eksik 60, faturalanacak 160
        FireFile open = file("1000", "0", "904", "0.04", "100", "0", "100");
        FireFile claimPartly = file("1000", "0", "904", "0.04", "100", "0", "30");
        FireFile claimDone = file("1000", "0", "904", "0.04", "100", "30", "0");
        FireFile done = file("1000", "0", "904", "0.04", "100", "60", "0");

        // then
        assertThat(open.toInvoiceKg().toPlainString()).isEqualTo("160.000");
        assertThat(open.actualFireRate().toPlainString()).isEqualTo("0.1067");
        assertThat(claimPartly.toInvoiceKg().toPlainString()).isEqualTo("90.000");
        assertThat(claimPartly.actualFireRate().toPlainString()).isEqualTo("0.1067");
        assertThat(claimDone.toInvoiceKg().toPlainString()).isEqualTo("30.000");
        assertThat(claimDone.actualFireRate().toPlainString()).isEqualTo("0.0733");
        assertThat(done.toInvoiceKg().toPlainString()).isEqualTo("0.000");
        assertThat(done.actualFireRate().toPlainString()).isEqualTo("0.0400");
    }

    @Test
    @DisplayName("Gönderilen ipliğin tamamı döndüyse ya da fatura eksiği aştıysa sonuç sıfırdır, eksiye düşmez")
    void shouldNeverDivideByZeroOrGoNegative() {
        // when
        FireFile allReturned = file("750.250", "750.250", "0", "0.04", "0", "0", "0");
        FireFile overInvoiced = file("1000", "0", "940", "0.04", "0", "60", "0");

        // then
        assertThat(allReturned.actualFireRate().toPlainString()).isEqualTo("0.0000");
        assertThat(allReturned.toInvoiceKg().toPlainString()).isEqualTo("0.000");
        assertThat(overInvoiced.shortageKg().toPlainString()).isEqualTo("20.000");
        assertThat(overInvoiced.toInvoiceKg().toPlainString()).isEqualTo("0.000");
    }
}
