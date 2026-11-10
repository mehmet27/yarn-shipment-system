package com.yarnshipment.shipment.domain.fabric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FabricReceiptTest {

    private static final int ROLLS = 4;

    private static FabricReceipt receipt(String netKg, @Nullable Integer rollCount, List<YarnShare> yarns) {
        return new FabricReceipt(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                " Süprem ",
                new BigDecimal(netKg),
                rollCount,
                " KIR-1 ",
                Instant.parse("2026-11-12T09:00:00Z"),
                yarns,
                UUID.randomUUID());
    }

    @Test
    @DisplayName("İplik toplamı kumaş net kg'sine eşitse kayıt kabul edilir ve metinler kırpılır")
    void shouldAcceptWhenYarnsSumToNetKg() {
        // when
        FabricReceipt result = receipt(
                "100",
                ROLLS,
                List.of(new YarnShare("A", new BigDecimal("60")), new YarnShare("B", new BigDecimal("40"))));

        // then
        assertThat(result.fabricType()).isEqualTo("Süprem");
        assertThat(result.waybillNo()).isEqualTo("KIR-1");
        assertThat(result.netKg().toPlainString()).isEqualTo("100.000");
    }

    @Test
    @DisplayName("İplik toplamı kumaş kilosuna eşit değilse reddedilir")
    void shouldRejectWhenYarnsDoNotSumToNetKg() {
        List<YarnShare> yarns = List.of(new YarnShare("A", new BigDecimal("60")));

        assertThatThrownBy(() -> receipt("100", null, yarns)).isInstanceOf(InvalidLedgerInputException.class);
    }

    @Test
    @DisplayName("Sıfır kilo, sıfır top sayısı ve iplik içermeyen kayıt reddedilir")
    void shouldRejectInvalidFields() {
        List<YarnShare> yarns = List.of(new YarnShare("A", new BigDecimal("10")));

        assertThatThrownBy(() -> receipt("0", null, yarns)).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> receipt("10", 0, yarns)).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> receipt("10", null, List.of())).isInstanceOf(InvalidLedgerInputException.class);
    }
}
