package com.yarnshipment.discrepancy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ToleranceBandsTest {

    @Test
    @DisplayName("Sınır değerler alt banda dahildir; işaret yok sayılır")
    void shouldClassifyInclusiveBoundaries() {
        ToleranceBands bands = ToleranceBands.REPORT_DEFAULT;

        assertThat(bands.classify(new BigDecimal("0.50"))).isEqualTo(Band.GREEN);
        assertThat(bands.classify(new BigDecimal("-0.51"))).isEqualTo(Band.YELLOW);
        assertThat(bands.classify(new BigDecimal("1.50"))).isEqualTo(Band.YELLOW);
        assertThat(bands.classify(new BigDecimal("1.51"))).isEqualTo(Band.RED);
    }

    @Test
    @DisplayName("Sarı sınırı yeşilden büyük, yeşil sınırı negatif olmayan olmalıdır")
    void shouldRejectInvalidBands() {
        assertThatThrownBy(() -> new ToleranceBands(BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToleranceBands(BigDecimal.ONE.negate(), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(
            "Ayar ekranından gelen bant 0 ≤ Yeşil < Sarı ≤ %100 ve en çok iki ondalık olmalıdır; hatalı alan söylenir")
    void shouldValidateSettingBands() {
        assertThat(ToleranceBands.ofSetting(new BigDecimal("0.8"), new BigDecimal("100.00"))
                        .yellowMaxPct())
                .isEqualByComparingTo("100");
        assertThatThrownBy(() -> ToleranceBands.ofSetting(new BigDecimal("-0.1"), BigDecimal.ONE))
                .isInstanceOfSatisfying(
                        InvalidSettingException.class,
                        e -> assertThat(e.field()).isEqualTo("greenMaxPct"));
        assertThatThrownBy(() -> ToleranceBands.ofSetting(new BigDecimal("0.125"), BigDecimal.ONE))
                .isInstanceOfSatisfying(
                        InvalidSettingException.class,
                        e -> assertThat(e.field()).isEqualTo("greenMaxPct"));
        assertThatThrownBy(() -> ToleranceBands.ofSetting(BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOfSatisfying(
                        InvalidSettingException.class,
                        e -> assertThat(e.field()).isEqualTo("yellowMaxPct"));
        assertThatThrownBy(() -> ToleranceBands.ofSetting(BigDecimal.ONE, new BigDecimal("100.01")))
                .isInstanceOf(InvalidSettingException.class);
    }
}
