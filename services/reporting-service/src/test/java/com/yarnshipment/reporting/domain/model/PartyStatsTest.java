package com.yarnshipment.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Oran hesabı. */
class PartyStatsTest {

    private static final int THREE = 3;

    @Test
    @DisplayName("Uyuşmazlık oranı sevkiyat bazında, iki ondalıkla yuvarlanır")
    void shouldComputeRatePerShipment() {
        PartyStats stats = new PartyStats(UUID.randomUUID(), THREE, 1);

        assertThat(stats.discrepancyRate()).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("Sevkiyat yoksa oran sıfırdır")
    void shouldReturnZeroWithoutShipments() {
        PartyStats stats = new PartyStats(UUID.randomUUID(), 0, 0);

        assertThat(stats.discrepancyRate()).isEqualByComparingTo("0");
    }
}
