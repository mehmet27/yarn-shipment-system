package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Çok iplikli sevkiyatın tamamlanması: reçete satırları fasoncu bakiyesine ve merkez stoka iplik cinsi başına yazılır.
 * Gönderilen kilo çıkış tartısının netidir; reçete toplamı tartıdan (kantar toleransı içinde) farklı olsa da iplik
 * cinsleri korunur ve cinslerin toplamı tartı netine gramı gramına eşittir.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BalanceMovementWriter.class, BalanceLedgerAdapter.class, CentralStockAdapter.class})
class DispatchLedgerRecipeTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000a01");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000a02");
    private static final UUID SOURCE_SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000a03");
    private static final UUID COTTON = UUID.fromString("0192f0a0-0000-7000-8000-000000000a04");
    private static final UUID LOT = UUID.fromString("0192f0a0-0000-7000-8000-000000000a05");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-19T09:00:00Z");
    private static final String COTTON_NAME = "Pamuk 30/1";
    private static final String POLYESTER = "Polyester 150";
    private static final String VISCOSE = "Viskon 30/1";
    private static final String INSERT_PRODUCT = """
            INSERT INTO md_product (id, code, name, active, created_at, created_by)
            VALUES (?, 'PAM-30', ?, true, now(), ?)""";
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  source_subcontractor_id, source_subcontractor_title, created_at, created_by)
            VALUES (?, ?, 'SV-R1', ?, 'Örnek Tekstil', 'DRAFT', ?, ?, now(), ?)""";
    private static final String INSERT_LINE = """
            INSERT INTO shipment_line (id, shipment_id, line_no, yarn_type_id, yarn_type_text, lot_id, net_kg,
                                       created_at, created_by)
            VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, now(), ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                                  created_at, created_by)
            VALUES (gen_random_uuid(), ?, 'DISPATCH', ?, 0, 40, now(), ?, now(), ?)""";

    @Autowired
    private BalanceLedgerAdapter ledger;

    @Autowired
    private CentralStockAdapter stock;

    @Autowired
    private JdbcTemplate jdbc;

    private int nextLineNo;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertProduct() {
        jdbc.update(INSERT_PRODUCT, COTTON, COTTON_NAME, USER);
        nextLineNo = 1;
    }

    private void givenShipment(boolean fromSubcontractor) {
        jdbc.update(
                INSERT_SHIPMENT,
                SHIPMENT,
                TestTokens.SITE_ID,
                SUBCONTRACTOR,
                fromSubcontractor ? SOURCE_SUBCONTRACTOR : null,
                fromSubcontractor ? "Kaynak Dokuma" : null,
                USER);
    }

    /** Listeden seçilen iplik (tanım kimliğiyle) reçete satırı. */
    private void givenProductLine(String netKg) {
        jdbc.update(INSERT_LINE, SHIPMENT, nextLineNo++, COTTON, null, LOT, new BigDecimal(netKg), USER);
    }

    /** Elle yazılan iplik cinsiyle reçete satırı. */
    private void givenTextLine(String yarnType, String netKg) {
        jdbc.update(INSERT_LINE, SHIPMENT, nextLineNo++, null, yarnType, null, new BigDecimal(netKg), USER);
    }

    private void givenWeighedNet(String netKg) {
        jdbc.update(INSERT_WEIGHING, SHIPMENT, new BigDecimal(netKg), USER, USER);
    }

    private Map<String, String> balancesOf(UUID subcontractorId) {
        return ledger.findBalances(subcontractorId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        BalanceRow::yarnType, row -> row.balanceKg().toPlainString()));
    }

    private BigDecimal totalBalanceOf(UUID subcontractorId) {
        return ledger.findBalances(subcontractorId).stream()
                .map(BalanceRow::balanceKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, String> centralStock() {
        return stock.findStock().stream()
                .collect(java.util.stream.Collectors.toMap(
                        CentralStockRow::yarnType, row -> row.stockKg().toPlainString()));
    }

    @Test
    @DisplayName("Reçete tartıya eşitse her iplik kendi kilosuyla bakiyeye ve merkez stoka yazılır; lot korunur")
    void shouldWriteOneMovementPerRecipeLineWhenRecipeEqualsWeighing() {
        // given: 600,500 + 399,500 = 1.000,000 kg; tartı neti 1.000,000 kg
        givenShipment(false);
        givenProductLine("600.500");
        givenTextLine(POLYESTER, "399.500");
        givenWeighedNet("1000.000");

        // when
        BigDecimal sentKg = ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        assertThat(sentKg.toPlainString()).isEqualTo("1000.000");
        assertThat(balancesOf(SUBCONTRACTOR))
                .containsOnly(Map.entry(COTTON_NAME, "600.500"), Map.entry(POLYESTER, "399.500"));
        assertThat(centralStock()).containsOnly(Map.entry(COTTON_NAME, "-600.500"), Map.entry(POLYESTER, "-399.500"));
        assertThat(jdbc.queryForList(
                        "SELECT lot_id FROM balance_movement WHERE shipment_id = ? AND lot_id IS NOT NULL",
                        UUID.class,
                        SHIPMENT))
                .containsExactly(LOT);
    }

    @Test
    @DisplayName("Tartı reçeteden tolerans içinde farklıysa iplik cinsleri korunur; toplam tartı netine eşittir")
    void shouldKeepYarnTypesWhenWeighingDiffersFromRecipeWithinTolerance() {
        // given: reçete 600 + 400 = 1.000 kg, tartı neti 999,400 kg (kantar toleransı içinde yola çıkmış)
        givenShipment(false);
        givenProductLine("600.000");
        givenTextLine(POLYESTER, "400.000");
        givenWeighedNet("999.400");

        // when
        BigDecimal sentKg = ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then: 999,400 x 0,6 = 599,640 ve 999,400 x 0,4 = 399,760
        assertThat(sentKg.toPlainString()).isEqualTo("999.400");
        assertThat(balancesOf(SUBCONTRACTOR))
                .containsOnly(Map.entry(COTTON_NAME, "599.640"), Map.entry(POLYESTER, "399.760"));
        assertThat(totalBalanceOf(SUBCONTRACTOR)).isEqualByComparingTo(sentKg);
        assertThat(centralStock()).containsOnly(Map.entry(COTTON_NAME, "-599.640"), Map.entry(POLYESTER, "-399.760"));
        assertThat(ledger.findDispatchedByYarn(SHIPMENT).keySet()).containsExactly(COTTON.toString(), POLYESTER);
    }

    @Test
    @DisplayName("Bölünemeyen gramlar satırlara dağıtılır; hiçbir gram kaybolmaz ve tekrar çağrı yeni hareket yazmaz")
    void shouldDistributeIndivisibleGramsWithoutLosingAny() {
        // given: üç eşit satır (333,333 x 3 = 999,999 kg), tartı neti 1.000,001 kg
        givenShipment(false);
        givenProductLine("333.333");
        givenTextLine(POLYESTER, "333.333");
        givenTextLine(VISCOSE, "333.333");
        givenWeighedNet("1000.001");

        // when
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then: 1.000.001 g / 3 = 333.333 g, artan 2 gram ilk iki satıra verilir
        assertThat(balancesOf(SUBCONTRACTOR))
                .containsOnly(
                        Map.entry(COTTON_NAME, "333.334"), Map.entry(POLYESTER, "333.334"), Map.entry(VISCOSE, "333.333"));
        assertThat(totalBalanceOf(SUBCONTRACTOR)).isEqualByComparingTo("1000.001");
        assertThat(stock.findStock().stream().map(CentralStockRow::stockKg).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("-1000.001");
    }

    @Test
    @DisplayName("Aynı iplik iki satırda geçse de her satır kendi payını alır; cins toplamı doğru çıkar")
    void shouldScaleEachLineWhenSameYarnAppearsTwice() {
        // given: Polyester 250 + Pamuk 500 + Polyester 250 = 1.000 kg, tartı neti 998 kg
        givenShipment(false);
        givenTextLine(POLYESTER, "250.000");
        givenProductLine("500.000");
        givenTextLine(POLYESTER, "250.000");
        givenWeighedNet("998.000");

        // when
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then: 998 x 0,25 = 249,5 (iki satır = 499) ve 998 x 0,5 = 499
        assertThat(balancesOf(SUBCONTRACTOR))
                .containsOnly(Map.entry(COTTON_NAME, "499.000"), Map.entry(POLYESTER, "499.000"));
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM balance_movement WHERE shipment_id = ? AND kind = 'DISPATCH'",
                        Integer.class,
                        SHIPMENT))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("Fasoncudan fasoncuya çok iplikli sevkiyatta gönderen her iplikten aynı kiloyu verir; merkez stok değişmez")
    void shouldMoveEachYarnBetweenSubcontractorsWithoutTouchingCentralStock() {
        // given
        givenShipment(true);
        givenProductLine("120.250");
        givenTextLine(POLYESTER, "79.750");
        givenWeighedNet("200.000");

        // when
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        assertThat(balancesOf(SUBCONTRACTOR))
                .containsOnly(Map.entry(COTTON_NAME, "120.250"), Map.entry(POLYESTER, "79.750"));
        assertThat(balancesOf(SOURCE_SUBCONTRACTOR))
                .containsOnly(Map.entry(COTTON_NAME, "-120.250"), Map.entry(POLYESTER, "-79.750"));
        assertThat(totalBalanceOf(SUBCONTRACTOR).add(totalBalanceOf(SOURCE_SUBCONTRACTOR)))
                .isEqualByComparingTo("0");
        assertThat(stock.findStock()).isEmpty();
    }

    @Test
    @DisplayName("Reçete satırı olmayan (eski) sevkiyat tartı netiyle tek satır yazılır")
    void shouldFallBackToSingleLineOnlyWithoutRecipe() {
        // given
        givenShipment(false);
        givenWeighedNet("500.000");

        // when
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        assertThat(balancesOf(SUBCONTRACTOR))
                .containsOnly(Map.entry(BalanceLedgerAdapter.UNSPECIFIED_YARN, "500.000"));
        assertThat(List.copyOf(ledger.findDispatchedByYarn(SHIPMENT).keySet()))
                .containsExactly(BalanceLedgerAdapter.UNSPECIFIED_YARN);
    }
}
