package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.in.fabric.FabricDrawEntry;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner;
import com.yarnshipment.shipment.domain.fabric.YarnDrawPlanner.Plan;
import com.yarnshipment.shipment.domain.fabric.YarnRatio;
import com.yarnshipment.shipment.domain.fabric.YarnStockRow;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Kumaş fasoncunun birden çok sevkiyatındaki iplikten düşer: kalan iplik, düşüm satırları, bakiye ve fire dosyası
 * gerçek PostgreSQL ve uygulama rolünün yetkileriyle denenir.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    BalanceMovementWriter.class,
    FabricReceiptAdapter.class,
    FabricReceiptReader.class,
    YarnStockAdapter.class,
    FireSettlementAdapter.class
})
// İstisna: CPLX-09/CPLX-10, senaryo testi alanın tüm port sahtelerini ve domain tiplerini birlikte kurar (GIT-10)
@SuppressWarnings({
    "checkstyle:ClassFanOutComplexity",
    "checkstyle:ClassDataAbstractionCoupling",
    "checkstyle:ParameterNumber"
})
class FabricYarnDrawAdapterTest {

    private static final int STOCK_ROWS = 3;
    private static final String RETURN_SQL = """
            INSERT INTO balance_movement (id, subcontractor_id, yarn_type, lot_id, shipment_id, kind, net_kg,
                                          occurred_at, created_by, source_ref)
            VALUES (?, ?, ?, NULL, ?, 'YARN_RETURN', -30, now(), ?, 'T:ret')""";

    private static final UUID OLDER = UUID.fromString("0192f0a0-0000-7000-8000-000000000a01");
    private static final UUID NEWER = UUID.fromString("0192f0a0-0000-7000-8000-000000000a02");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000a03");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID SITE = TestTokens.SITE_ID;
    private static final UUID PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-000000000a04");
    private static final UUID GRADE = UUID.fromString("0192f0a0-0000-7000-8000-000000000a05");
    private static final UUID LOT = UUID.fromString("0192f0a0-0000-7000-8000-000000000a06");
    private static final Instant T1 = Instant.parse("2026-11-01T08:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-02T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");
    private static final String PENYE = "30/1 penye";

    @Autowired
    private FabricReceiptAdapter fabricReceipts;

    @Autowired
    private YarnStockAdapter stock;

    @Autowired
    private FireSettlementAdapter fireSettlements;

    @MockitoBean
    private LedgerEventPort ledgerEvents;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void givenTwoShipmentsWithDispatchedYarn() {
        shipment(OLDER, "SV-OLD", T1);
        shipment(NEWER, "SV-NEW", T2);
        jdbc.update(
                "INSERT INTO md_site (id, name, created_at, created_by) VALUES (?, 'T', now(), ?)"
                        + " ON CONFLICT DO NOTHING",
                SITE,
                USER);
        dispatch(OLDER, PENYE, null, "300", "a");
        dispatch(OLDER, "20/1 open end", null, "100", "b");
        dispatch(NEWER, PENYE, null, "200", "c");
    }

    private void shipment(UUID id, String no, Instant at) {
        jdbc.update("""
                INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                      created_at, created_by)
                VALUES (?, ?, ?, ?, 'Örnek Tekstil', 'COMPLETED', now(), ?)""", id, SITE, no, SUBCONTRACTOR, USER);
        fireSettlements.open(FireSettlement.open(id, SUBCONTRACTOR, new BigDecimal("1000")), at);
    }

    private void dispatch(UUID shipment, String yarn, @Nullable UUID lot, String kg, String ref) {
        jdbc.update(
                """
                INSERT INTO balance_movement (id, subcontractor_id, yarn_type, lot_id, shipment_id, kind, net_kg,
                                              occurred_at, created_by, source_ref)
                VALUES (?, ?, ?, ?, ?, 'DISPATCH', ?, now(), ?, ?)""",
                UUID.randomUUID(),
                SUBCONTRACTOR,
                yarn,
                lot,
                shipment,
                new BigDecimal(kg),
                USER,
                "T:" + shipment + ref);
    }

    private FabricReceipt receiptOver(UUID entryShipment, String kg, Plan plan) {
        return new FabricReceipt(
                UUID.randomUUID(),
                entryShipment,
                SUBCONTRACTOR,
                "Süprem",
                new BigDecimal(kg),
                null,
                "KIR-1",
                NOW,
                plan.yarns(),
                USER);
    }

    @Test
    @DisplayName(
            "Kalan iplik fasoncunun açık sevkiyatlarından okunur; kumaş girildiği sevkiyattan başlayıp eskiye taşar")
    void shouldDrawAcrossShipmentsAndFeedFireFiles() {
        // given
        List<YarnStockRow> before = stock.findOpenStock(SUBCONTRACTOR);
        assertThat(before).hasSize(STOCK_ROWS);

        // when: 450 kg penye, kumaş yeni sevkiyattan girilir (yeni: 200 kg, eski: 300 kg)
        Plan plan = YarnDrawPlanner.plan(
                NEWER, new BigDecimal("450"), List.of(new YarnRatio(PENYE, null, new BigDecimal("100"))), before);
        assertThat(plan.isFeasible()).isTrue();
        FabricReceipt receipt = receiptOver(NEWER, "450", plan);
        fabricReceipts.save(receipt);

        // then: iki sevkiyata dağılmış düşüm, bakiye ve fire dosyası
        assertThat(fireSettlements.totals(NEWER).fabricKg()).isEqualByComparingTo("200");
        assertThat(fireSettlements.totals(OLDER).fabricKg()).isEqualByComparingTo("250");
        assertThat(stock.findOpenStock(SUBCONTRACTOR))
                .filteredOn(row -> row.yarnName().equals(PENYE))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.shipmentId()).isEqualTo(OLDER);
                    assertThat(row.availableKg()).isEqualByComparingTo("50");
                });
        assertThat(fabricReceipts.findByShipment(NEWER)).singleElement().satisfies(found -> {
            assertThat(found.yarns()).singleElement().satisfies(yarn -> {
                assertThat(yarn.ratioPct()).isEqualByComparingTo("100");
                assertThat(yarn.draws()).hasSize(2);
                assertThat(yarn.draws().get(0).shipmentNo()).isEqualTo("SV-NEW");
                assertThat(yarn.draws().get(1).shipmentNo()).isEqualTo("SV-OLD");
            });
        });
        List<FabricDrawEntry> drawn = fabricReceipts.findDrawsFrom(OLDER);
        assertThat(drawn).singleElement().satisfies(entry -> {
            assertThat(entry.entryShipmentNo()).isEqualTo("SV-NEW");
            assertThat(entry.fabricType()).isEqualTo("Süprem");
            assertThat(entry.netKg()).isEqualByComparingTo("250");
        });
    }

    @Test
    @DisplayName("Fire dosyası kapanmış sevkiyatın ipliği kalan iplik sayılmaz")
    void shouldIgnoreFinalFiles() {
        jdbc.update("UPDATE fire_settlement SET status = 'CLOSED' WHERE shipment_id = ?", OLDER);

        assertThat(stock.findOpenStock(SUBCONTRACTOR))
                .extracting(YarnStockRow::shipmentId)
                .containsOnly(NEWER);
    }

    @Test
    @DisplayName(
            "Partili iplik ayrı satırdır: tedarikçisi ve parti numarası okunur; partisiz kayıtlar dönen ipliği bölüşür")
    void shouldListLotsAsSeparateRows() {
        // given
        jdbc.update(
                "INSERT INTO md_product (id, code, name, active, created_at, created_by)"
                        + " VALUES (?, 'PNY', 'Penye', true, now(), ?)",
                PRODUCT,
                USER);
        jdbc.update(
                "INSERT INTO md_quality_grade (id, code, name, created_at, created_by)"
                        + " VALUES (?, 'QA', 'A', now(), ?)",
                GRADE,
                USER);
        jdbc.update(
                "INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id, production_date,"
                        + " created_at, created_by) VALUES (?, ?, 'PL-7', ?, ?, current_date, now(), ?)",
                LOT,
                SITE,
                PRODUCT,
                GRADE,
                USER);
        dispatch(NEWER, PRODUCT.toString(), LOT, "80", "d");
        // dönen iplik partisiz yazılır
        jdbc.update(RETURN_SQL, UUID.randomUUID(), SUBCONTRACTOR, PRODUCT.toString(), NEWER, USER);

        // when
        List<YarnStockRow> rows = stock.findOpenStock(SUBCONTRACTOR);

        // then
        assertThat(rows).filteredOn(row -> row.lotId() != null).singleElement().satisfies(row -> {
            assertThat(row.yarnName()).isEqualTo("Penye");
            assertThat(row.lotNo()).isEqualTo("PL-7");
            assertThat(row.availableKg()).isEqualByComparingTo("50");
        });
    }
}
