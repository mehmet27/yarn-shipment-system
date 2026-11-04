package com.yarnshipment.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Oran ve ortalama hesapları. */
class PartyStatsTest {

    private static final int THREE = 3;
    private static final int FOUR = 4;

    @Test
    @DisplayName("Uyuşmazlık oranı sevkiyat bazında, iki ondalıkla yuvarlanır")
    void shouldComputeRatePerShipment() {
        PartyStats stats = new PartyStats(UUID.randomUUID(), THREE, 1, new BigDecimal("30.000"));

        assertThat(stats.discrepancyRate()).isEqualByComparingTo("33.33");
        assertThat(stats.averageAbsoluteDiffKg()).isEqualByComparingTo("10.000");
    }

    @Test
    @DisplayName("Sevkiyat yoksa oran ve ortalama sıfırdır")
    void shouldReturnZeroWithoutShipments() {
        PartyStats stats = new PartyStats(UUID.randomUUID(), 0, 0, BigDecimal.ZERO);

        assertThat(stats.discrepancyRate()).isEqualByComparingTo("0");
        assertThat(stats.averageAbsoluteDiffKg()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Eşleşme oranı ve ortalaması aynı kurallarla hesaplanır")
    void shouldComputePairRate() {
        PairStats pair = new PairStats(UUID.randomUUID(), UUID.randomUUID(), FOUR, 1, 2, new BigDecimal("10.000"));

        assertThat(pair.discrepancyRate()).isEqualByComparingTo("25.00");
        assertThat(pair.averageAbsoluteDiffKg()).isEqualByComparingTo("2.500");
    }
}
