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
}
