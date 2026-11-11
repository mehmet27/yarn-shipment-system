package com.yarnshipment.shipment.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CentralProductionTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-13T09:00:00Z");

    private static CentralProduction production(String netKg, String fire, List<YarnShare> yarns) {
        return new CentralProduction(
                UUID.randomUUID(), "Süprem", new BigDecimal(netKg), new BigDecimal(fire), null, null, AT, yarns, USER);
    }

    @Test
    @DisplayName("100 kg kumaş %4 fire ile 104,167 kg iplik düşer")
    void shouldAddFireToConsumption() {
        // when
        CentralProduction production = production("100", "0.04", List.of(new YarnShare("A", new BigDecimal("100"))));

        // then
        assertThat(production.consumptions()).singleElement().satisfies(movement -> {
            assertThat(movement.netKg()).isEqualByComparingTo("-104.167");
            assertThat(movement.fabricKg()).isEqualByComparingTo("100");
            assertThat(movement.kind()).isEqualTo(CentralStockKind.PRODUCTION);
        });
    }

    @Test
    @DisplayName("İplik payları kumaş kilosuna eşit değilse ya da fire sınırı aşılırsa reddedilir")
    void shouldRejectMismatchedYarnsAndHighFire() {
        List<YarnShare> yarns = List.of(new YarnShare("A", new BigDecimal("90")));

        assertThatThrownBy(() -> production("100", "0.04", yarns))
                .isInstanceOf(InvalidLedgerInputException.class)
                .hasMessageContaining("yarns");
        assertThatThrownBy(() -> production("90", "0.25", yarns))
                .isInstanceOf(InvalidLedgerInputException.class)
                .hasMessageContaining("fireRate");
    }

    @Test
    @DisplayName("Giriş artı, çıkış eksi işaretli olmalıdır")
    void shouldRejectWrongSign() {
        assertThatThrownBy(() -> new CentralStockMovement(
                        UUID.randomUUID(),
                        "A",
                        CentralStockKind.YARN_RECEIPT,
                        new BigDecimal("-1"),
                        null,
                        null,
                        null,
                        AT,
                        USER))
                .isInstanceOf(InvalidLedgerInputException.class);
    }
}
