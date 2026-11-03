package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TareBasisTest {

    private static final int BAG_COUNT = 40;
    private static final TareBasis BASIS = new TareBasis(new BigDecimal("20.000"), new BigDecimal("5.00"));

    @Test
    @DisplayName("Beklenen dara standart × çuval sayısıdır")
    void shouldMultiplyStandardByBagCount() {
        TareBasis basis = Objects.requireNonNull(TareBasis.of(new BigDecimal("0.450"), new BigDecimal("5"), BAG_COUNT));

        assertThat(basis.expectedKg()).isEqualByComparingTo("18.000");
        assertThat(basis.tolerancePct()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Standart sıfırsa karşılaştırma yapılmaz")
    void shouldSkipZeroStandard() {
        assertThat(TareBasis.of(BigDecimal.ZERO, new BigDecimal("5"), BAG_COUNT))
                .isNull();
    }

    @Test
    @DisplayName("Sapma işaretlidir: hafif dara eksi, ağır dara artı")
    void shouldSignDeviation() {
        assertThat(BASIS.deviationPct(new BigDecimal("18.000"))).isEqualByComparingTo("-10.00");
        assertThat(BASIS.deviationPct(new BigDecimal("21.000"))).isEqualByComparingTo("5.00");
        assertThat(BASIS.deviationPct(new BigDecimal("20.000"))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Sınırın tam üzerinde uyarı yok, aşınca uyarı var; iki yönde de")
    void shouldAlertOnlyBeyondTolerance() {
        assertThat(BASIS.exceeded(new BigDecimal("21.000"))).isFalse();
        assertThat(BASIS.exceeded(new BigDecimal("19.000"))).isFalse();
        assertThat(BASIS.exceeded(new BigDecimal("21.200"))).isTrue();
        assertThat(BASIS.exceeded(new BigDecimal("18.000"))).isTrue();
    }

    @Test
    @DisplayName("Beklenen dara sıfırsa sapma sıfırdır, bölme hatası olmaz")
    void shouldHandleZeroExpected() {
        TareBasis zero = new TareBasis(BigDecimal.ZERO, new BigDecimal("5.00"));

        assertThat(zero.deviationPct(new BigDecimal("3"))).isEqualByComparingTo("0");
        assertThat(zero.exceeded(new BigDecimal("3"))).isFalse();
    }

    @Test
    @DisplayName("Negatif değerler reddedilir")
    void shouldRejectNegative() {
        assertThatThrownBy(() -> new TareBasis(new BigDecimal("-1"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TareBasis(BigDecimal.ONE, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
