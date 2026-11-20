package com.yarnshipment.shipment.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Merkez üretiminde stoktan düşen iplik: kumaş / (1 - fire), iplik başına gram hassasiyetinde. */
class CentralProductionRoundingTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID COTTON = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID POLYESTER = UUID.fromString("0192f0a0-0000-7000-8000-00000000a002");
    private static final UUID VISCOSE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a003");
    private static final Instant AT = Instant.parse("2026-11-19T09:00:00Z");

    private static CentralProduction production(String fire, String... yarnKg) {
        UUID[] products = {COTTON, POLYESTER, VISCOSE};
        List<ProductShare> yarns = new java.util.ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < yarnKg.length; i++) {
            BigDecimal kg = new BigDecimal(yarnKg[i]);
            yarns.add(new ProductShare(products[i], "İplik " + i, kg));
            total = total.add(kg);
        }
        return new CentralProduction(
                UUID.randomUUID(), "Süprem", total, new BigDecimal(fire), null, null, AT, yarns, USER);
    }

    private static List<String> consumed(CentralProduction production) {
        return production.consumptions().stream()
                .map(movement -> movement.netKg().toPlainString())
                .toList();
    }

    @Test
    @DisplayName("Çok iplikli üretimde her iplik kendi payının fire dahil kilosu kadar düşer")
    void shouldDeductEachYarnWithFire() {
        // when: 60 / 0,96 = 62,500 ve 40 / 0,96 = 41,6667
        CentralProduction production = production("0.04", "60", "40");

        // then
        assertThat(consumed(production)).containsExactly("-62.500", "-41.667");
        assertThat(production.consumptions())
                .extracting(CentralStockMovement::fabricKg)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("60"), new BigDecimal("40"));
    }

    @Test
    @DisplayName("Üç eşit iplikte düşen toplam, kumaşın fire dahil toplamıyla aynı gramdır")
    void shouldNotDriftAcrossYarns() {
        // when: 33,333 / 0,96 = 34,7219; 33,334 / 0,96 = 34,7229; toplam 100 / 0,96 = 104,1667
        CentralProduction production = production("0.04", "33.333", "33.333", "33.334");

        // then
        assertThat(consumed(production)).containsExactly("-34.722", "-34.722", "-34.723");
        assertThat(production.consumptions().stream()
                        .map(CentralStockMovement::netKg)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("-104.167");
    }

    @Test
    @DisplayName("Fire sıfırsa kumaş kadar, yüzde 20 ise kumaşın 1,25 katı iplik düşer")
    void shouldApplyFireRateBounds() {
        assertThat(consumed(production("0", "100"))).containsExactly("-100.000");
        assertThat(consumed(production("0.20", "100"))).containsExactly("-125.000");
    }

    @Test
    @DisplayName("Fire oranı eksi ya da yüzde 20'den büyük olamaz")
    void shouldRejectFireRateOutOfRange() {
        assertThatThrownBy(() -> production("-0.01", "100"))
                .isInstanceOf(InvalidLedgerInputException.class)
                .hasMessageContaining("fireRate");
        assertThatThrownBy(() -> production("0.2001", "100"))
                .isInstanceOf(InvalidLedgerInputException.class)
                .hasMessageContaining("fireRate");
    }
}
