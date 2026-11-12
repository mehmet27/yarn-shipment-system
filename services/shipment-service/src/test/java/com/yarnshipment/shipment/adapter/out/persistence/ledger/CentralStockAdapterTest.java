package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Merkez iplik stoku gerçek PostgreSQL ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BalanceMovementWriter.class, BalanceLedgerAdapter.class, CentralStockAdapter.class})
class CentralStockAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000801");
    private static final UUID TRANSFER_SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000802");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000803");
    private static final UUID SOURCE_SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000804");
    private static final UUID DIRECT_SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000805");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-13T09:00:00Z");
    private static final int RECENT_LIMIT = 10;
    private static final String YARN = "Ne 30/1 Penye";
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  source_subcontractor_id, source_subcontractor_title, created_at, created_by)
            VALUES (?, ?, ?, ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', ?, ?, now(), ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                                  created_at, created_by)
            VALUES (?, ?, 'DISPATCH', 34050, 18, 40, now(), ?, now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private BalanceLedgerAdapter ledger;

    @Autowired
    private CentralStockAdapter stock;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertShipments() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, TestTokens.SITE_ID, "SV-C1", SUBCONTRACTOR, null, null, USER);
        jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), SHIPMENT, USER, USER);
        jdbc.update(
                INSERT_SHIPMENT,
                TRANSFER_SHIPMENT,
                TestTokens.SITE_ID,
                "SV-C2",
                SUBCONTRACTOR,
                SOURCE_SUBCONTRACTOR,
                "Kaynak Dokuma",
                USER);
        jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), TRANSFER_SHIPMENT, USER, USER);
    }

    private BigDecimal stockOf(String yarn) {
        return stock.findStock().stream()
                .filter(row -> row.yarnType().equals(yarn))
                .map(CentralStockRow::stockKg)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Merkezden çıkan sevkiyat merkez stoktan bir kez düşer; fasoncudan fasoncuya sevkiyat düşmez")
    void shouldDeductCentralDispatchOnlyOnce() {
        // when
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        ledger.recordDispatch(TRANSFER_SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        assertThat(stockOf(BalanceLedgerAdapter.UNSPECIFIED_YARN)).isEqualByComparingTo("-34032");
    }

    @Test
    @DisplayName("İplik girişi stoğu artırır; üretim iplikleri fire ile düşer ve stok eksiye düşebilir")
    void shouldAddReceiptAndDeductProductionWithFire() {
        // given
        stock.saveReceipt(CentralStockMovement.receipt(
                new CentralStockMovement.Receipt(YARN, new BigDecimal("20000"), "Tedarikçi irsaliyesi"), AT, USER));
        CentralProduction production = new CentralProduction(
                UUID.randomUUID(),
                "Süprem",
                new BigDecimal("1000"),
                new BigDecimal("0.04"),
                null,
                null,
                AT,
                List.of(new YarnShare(YARN, new BigDecimal("1000"))),
                USER);

        // when
        stock.saveProduction(production, production.consumptions());

        // then
        assertThat(stockOf(YARN)).isEqualByComparingTo("18958.333");
        assertThat(stock.findRecent(RECENT_LIMIT)).hasSize(2);
    }

    @Test
    @DisplayName(
            "Kapıdan aktarmada aynı kilo giriş (+) ve çıkış (-) yazılır: stok eksiye düşmez, iz kalır, tekrar yazılmaz")
    void shouldWriteReceiptAndDispatchForDirectTransfer() {
        // given
        jdbc.update(INSERT_SHIPMENT, DIRECT_SHIPMENT, TestTokens.SITE_ID, "SV-C3", SUBCONTRACTOR, null, null, USER);
        jdbc.update(
                "UPDATE shipment SET direct_transfer_supplier = 'Esha İplik', direct_transfer_waybill_no = 'IRS-77'"
                        + " WHERE id = ?",
                DIRECT_SHIPMENT);
        jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), DIRECT_SHIPMENT, USER, USER);

        // when
        ledger.recordDispatch(DIRECT_SHIPMENT, SUBCONTRACTOR, AT, USER);
        ledger.recordDispatch(DIRECT_SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        assertThat(stockOf(BalanceLedgerAdapter.UNSPECIFIED_YARN)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForList(
                        "SELECT kind FROM central_stock_movement WHERE shipment_id = ? ORDER BY net_kg DESC",
                        String.class,
                        DIRECT_SHIPMENT))
                .containsExactly("YARN_RECEIPT", "TO_SUBCONTRACTOR");
        assertThat(jdbc.queryForObject(
                        "SELECT note FROM central_stock_movement WHERE shipment_id = ? AND kind = 'YARN_RECEIPT'",
                        String.class,
                        DIRECT_SHIPMENT))
                .contains("Esha İplik")
                .contains("IRS-77");
    }
}
