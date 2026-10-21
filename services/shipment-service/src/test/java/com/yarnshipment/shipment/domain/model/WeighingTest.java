package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeighingTest {

    private static final int BAG_COUNT = 40;
    private static final Instant WEIGHED_AT = Instant.parse("2026-10-21T07:00:00Z");

    @Test
    @DisplayName("Net kilo brüt eksi daradır")
    void shouldComputeNetWeight() {
        // given
        Weighing weighing = new Weighing(new BigDecimal("2000.500"), new BigDecimal("18.000"), BAG_COUNT, WEIGHED_AT);

        // when / then
        assertThat(weighing.netKg()).isEqualByComparingTo("1982.500");
    }

    @Test
    @DisplayName("Dara brütten büyük olamaz")
    void shouldRejectTareGreaterThanGross() {
        // when / then
        assertThatThrownBy(() -> new Weighing(BigDecimal.ONE, BigDecimal.TEN, 1, WEIGHED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Negatif dara ve sıfır çuval reddedilir")
    void shouldRejectNegativeTareAndZeroBags() {
        // when / then
        assertThatThrownBy(() -> new Weighing(BigDecimal.TEN, new BigDecimal("-1"), 1, WEIGHED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Weighing(BigDecimal.TEN, BigDecimal.ONE, 0, WEIGHED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Şoför adı toString çıktısında görünmez")
    void shouldMaskDriverNameInToString() {
        // given
        ShipmentParties parties =
                new ShipmentParties(UUID.randomUUID(), "Örnek Tekstil", null, "Ali Yılmaz", "34ABC123");

        // when / then
        assertThat(parties.toString()).doesNotContain("Ali");
    }
}
