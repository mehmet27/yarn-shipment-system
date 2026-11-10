package com.yarnshipment.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fire hesapları (analiz 8). */
class FireFileTest {

    private static FireFile file(String sent, String returned, String fabric, String rate) {
        return new FireFile(
                UUID.randomUUID(),
                "SHP-1",
                UUID.randomUUID(),
                "Kırıkhan",
                new BigDecimal(sent),
                new BigDecimal(returned),
                new BigDecimal(fabric),
                new BigDecimal(rate),
                "TO_INVOICE",
                null,
                null);
    }

    @Test
    @DisplayName("Kırıkhan: gerçekleşen fire %4,03, beklenen 32670,720, eksik 10,720")
    void shouldMatchKirikhanAcceptance() {
        FireFile f = file("34032.000", "0.000", "32660.000", "0.04");

        assertThat(f.actualFireRate()).isEqualByComparingTo("0.0403");
        assertThat(f.expectedFabricKg()).isEqualByComparingTo("32670.720");
        assertThat(f.shortageKg()).isEqualByComparingTo("10.720");
    }

    @Test
    @DisplayName("Fire içinde eksik yoktur; dönen iplik tabanı düşürür; özel oran uygulanır")
    void shouldHandleOtherCases() {
        assertThat(file("1000", "0", "960", "0.04").shortageKg()).isEqualByComparingTo("0");
        assertThat(file("1000", "100", "864", "0.04").shortageKg()).isEqualByComparingTo("0");
        assertThat(file("1000", "0", "900", "0.04").shortageKg()).isEqualByComparingTo("60");
        assertThat(file("1000", "0", "940", "0.05").shortageKg()).isEqualByComparingTo("10");
        assertThat(file("100", "100", "0", "0.04").actualFireRate()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Fasoncu toplamı oranı toplamlardan hesaplar")
    void shouldAggregate() {
        UUID sub = UUID.randomUUID();
        FireFile a = new FireFile(
                UUID.randomUUID(),
                "A",
                sub,
                "K",
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal("900"),
                new BigDecimal("0.04"),
                "TO_INVOICE",
                null,
                null);
        FireFile b = new FireFile(
                UUID.randomUUID(),
                "B",
                sub,
                "K",
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal("960"),
                new BigDecimal("0.04"),
                "CLOSED",
                null,
                null);

        List<YarnVsFabricRow> rows = YarnVsFabricRow.aggregate(List.of(a, b));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).actualFireRate()).isEqualByComparingTo("0.0700");
        assertThat(rows.get(0).shortageKg()).isEqualByComparingTo("60");
    }
}
