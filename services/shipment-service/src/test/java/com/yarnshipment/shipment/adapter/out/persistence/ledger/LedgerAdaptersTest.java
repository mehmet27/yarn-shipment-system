package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesQuery;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort.FireTotals;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.FabricYarn;
import com.yarnshipment.shipment.domain.fabric.ReturnDirection;
import com.yarnshipment.shipment.domain.fabric.YarnDraw;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Defter, kumaş girişi, iplik dönüşü ve fire dosyası gerçek PostgreSQL ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    BalanceMovementWriter.class,
    BalanceLedgerAdapter.class,
    FabricReceiptAdapter.class,
    FabricReceiptReader.class,
    YarnStockAdapter.class,
    YarnReturnAdapter.class,
    FireSettlementAdapter.class,
    FireInvoiceAdapter.class,
    FireInvoiceListAdapter.class,
    ClaimLedgerAdapter.class
})
// İstisna: CPLX-09/CPLX-10, senaryo testi alanın tüm port sahtelerini ve domain tiplerini birlikte kurar (GIT-10)
@SuppressWarnings({"checkstyle:ClassFanOutComplexity", "checkstyle:ClassDataAbstractionCoupling"})
class LedgerAdaptersTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000701");
    private static final UUID SITE = TestTokens.SITE_ID;
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000702");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final int ROLLS = 12;
    private static final int PAGE_SIZE = 10;
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-L1', ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', now(), ?)""";
    private static final UUID TRANSFER_SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000703");
    private static final UUID SOURCE_SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000704");
    private static final String INSERT_TRANSFER_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  source_subcontractor_id, source_subcontractor_title, created_at, created_by)
            VALUES (?, ?, 'SV-L2', ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', ?, 'Kaynak Dokuma', now(), ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                                  created_at, created_by)
            VALUES (?, ?, 'DISPATCH', 34050, 18, 40, now(), ?, now(), ?)""";

    private static final String INSERT_NAMED_PRODUCT = """
            INSERT INTO md_product (id, code, name, active, created_at, created_by)
            VALUES (?, 'PAM', 'Pamuk 20/1', true, now(), ?)""";
    private static final String INSERT_PRODUCT_MOVEMENT = """
            INSERT INTO balance_movement (id, subcontractor_id, yarn_type, shipment_id, kind, net_kg, occurred_at,
                                          created_by, source_ref)
            VALUES (?, ?, ?, ?, 'DISPATCH', 100, now(), ?, 'TEST:yarn-name')""";

    @Autowired
    private BalanceLedgerAdapter ledger;

    @Autowired
    private FabricReceiptAdapter fabricReceipts;

    @MockitoBean
    private LedgerEventPort ledgerEvents;

    @Autowired
    private YarnReturnAdapter yarnReturns;

    @Autowired
    private FireSettlementAdapter fireSettlements;

    @Autowired
    private FireInvoiceAdapter fireInvoices;

    @Autowired
    private FireInvoiceListAdapter invoiceList;

    @Autowired
    private ClaimLedgerAdapter claims;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertShipmentWithDispatchWeighing() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, SUBCONTRACTOR, USER);
        jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), SHIPMENT, USER, USER);
    }

    private BigDecimal balanceOf(String yarnType) {
        return ledger.findBalances(SUBCONTRACTOR).stream()
                .filter(row -> row.yarnType().equals(yarnType))
                .map(BalanceRow::balanceKg)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Çıkış tartısı neti reçetesiz tek DISPATCH satırı olarak yazılır; tekrar çağrı yeni hareket üretmez")
    void shouldRecordDispatchOnceAsSingleLine() {
        // when
        BigDecimal first = ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        BigDecimal repeat = ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        assertThat(first).isEqualByComparingTo("34032.000");
        assertThat(repeat).isEqualByComparingTo("34032.000");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM balance_movement WHERE shipment_id = ? AND kind = 'DISPATCH'",
                        Integer.class,
                        SHIPMENT))
                .isEqualTo(1);
        assertThat(balanceOf("(reçetesiz)")).isEqualByComparingTo("34032");
    }

    @Test
    @DisplayName("Fasoncudan fasoncuya sevkiyatta alıcıya DISPATCH yazılır, gönderenin bakiyesinden aynı kilo düşer")
    void shouldDeductTransferFromSourceSubcontractor() {
        // given
        jdbc.update(INSERT_TRANSFER_SHIPMENT, TRANSFER_SHIPMENT, SITE, SUBCONTRACTOR, SOURCE_SUBCONTRACTOR, USER);
        jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), TRANSFER_SHIPMENT, USER, USER);

        // when
        ledger.recordDispatch(TRANSFER_SHIPMENT, SUBCONTRACTOR, AT, USER);
        ledger.recordDispatch(TRANSFER_SHIPMENT, SUBCONTRACTOR, AT, USER);

        // then
        BigDecimal sourceBalance = ledger.findBalances(SOURCE_SUBCONTRACTOR).stream()
                .map(BalanceRow::balanceKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receiverBalance = ledger.findBalances(SUBCONTRACTOR).stream()
                .map(BalanceRow::balanceKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sourceBalance).isEqualByComparingTo("-34032");
        assertThat(receiverBalance).isEqualByComparingTo("34032");
    }

    @Test
    @DisplayName("Kumaş girişi bakiyeden düşer; bakiye hareketlerin toplamıdır")
    void shouldDeductFabricReceiptFromBalance() {
        // given
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        FabricReceipt receipt = new FabricReceipt(
                UUID.randomUUID(),
                SHIPMENT,
                SUBCONTRACTOR,
                "Süprem",
                new BigDecimal("32660"),
                ROLLS,
                "KIR-1",
                AT,
                List.of(new FabricYarn(
                        "(reçetesiz)",
                        null,
                        null,
                        new BigDecimal("100.00"),
                        new BigDecimal("32660"),
                        List.of(new YarnDraw(SHIPMENT, null, "(reçetesiz)", null, new BigDecimal("32660"))))),
                USER);
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);

        // when
        fabricReceipts.save(receipt);

        // then
        assertThat(balanceOf("(reçetesiz)")).isEqualByComparingTo("1372");
        assertThat(fabricReceipts.findByShipment(SHIPMENT)).singleElement().satisfies(found -> {
            assertThat(found.id()).isEqualTo(receipt.id());
            assertThat(found.rollCount()).isEqualTo(ROLLS);
            assertThat(found.yarns()).singleElement().satisfies(this::assertSingleDrawYarn);
        });
        assertThat(fireSettlements.totals(SHIPMENT).fabricKg()).isEqualByComparingTo("32660");
    }

    private void assertSingleDrawYarn(FabricYarn yarn) {
        assertThat(yarn.yarnType()).isEqualTo("(reçetesiz)");
        assertThat(yarn.ratioPct()).isEqualByComparingTo("100");
        assertThat(yarn.draws())
                .singleElement()
                .satisfies(draw -> assertThat(draw.shipmentId()).isEqualTo(SHIPMENT));
    }

    @Test
    @DisplayName("Dönen iplik bakiyeden düşer ve fire dosyasında D olarak toplanır; fasoncudan gelen toplanmaz")
    void shouldSumOnlyReturnedYarnForFire() {
        // given
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);

        // when
        yarnReturns.save(yarnReturn(ReturnDirection.RETURN, "100"));
        yarnReturns.save(yarnReturn(ReturnDirection.FROM_SUBCONTRACTOR, "40"));

        // then
        FireTotals totals = fireSettlements.totals(SHIPMENT);
        assertThat(totals.returnedKg()).isEqualByComparingTo("100");
        assertThat(totals.claimKg()).isEqualByComparingTo("0");
        assertThat(yarnReturns.findByShipment(SHIPMENT)).hasSize(2);
        assertThat(balanceOf("30/1 penye")).isEqualByComparingTo("-140");
    }

    @Test
    @DisplayName("Hareketler sayfalı ve süzülmüş okunur")
    void shouldPageMovements() {
        // given
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        yarnReturns.save(yarnReturn(ReturnDirection.RETURN, "100"));

        // when
        MovementPage page = ledger.findMovements(
                new ListMovementsQuery(TestTokens.actor("manager"), SUBCONTRACTOR, "30/1 penye", 0, PAGE_SIZE));

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(movement -> {
            assertThat(movement.kind().name()).isEqualTo("YARN_RETURN");
            assertThat(movement.netKg()).isEqualByComparingTo("-100");
        });
    }

    @Test
    @DisplayName("Hareketlerde iplik kimliği yerine iplik adı görünür; ad ile süzülebilir")
    void shouldShowYarnNameInsteadOfProductId() {
        // given
        UUID product = UUID.fromString("0192f0a0-0000-7000-8000-00000000a201");
        jdbc.update(INSERT_NAMED_PRODUCT, product, USER);
        jdbc.update(INSERT_PRODUCT_MOVEMENT, UUID.randomUUID(), SUBCONTRACTOR, product.toString(), SHIPMENT, USER);

        // when
        MovementPage page = ledger.findMovements(
                new ListMovementsQuery(TestTokens.actor("manager"), SUBCONTRACTOR, "Pamuk 20/1", 0, PAGE_SIZE));

        // then
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items())
                .singleElement()
                .satisfies(movement -> assertThat(movement.yarn().yarnType()).isEqualTo("Pamuk 20/1"));
    }

    @Test
    @DisplayName("Fire dosyası bir kez açılır, oran ve durum yazılır, liste süzülür")
    void shouldOpenOnceAndSaveSettlement() {
        // given
        FireSettlement open = FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032"));
        fireSettlements.open(open, AT);
        fireSettlements.open(open.withFireRate(new BigDecimal("0.05")), AT);

        // when
        FireSettlement changed = fireSettlements
                .lock(SHIPMENT)
                .orElseThrow()
                .withFireRate(new BigDecimal("0.05"))
                .afterFabricReceipt();
        fireSettlements.save(changed, AT);

        // then
        assertThat(fireSettlements.find(SHIPMENT)).get().satisfies(found -> {
            assertThat(found.status()).isEqualTo(FireSettlementStatus.TO_INVOICE);
            assertThat(found.fireRate()).isEqualByComparingTo("0.05");
        });
        ListFireSettlementsQuery query = new ListFireSettlementsQuery(
                TestTokens.actor("manager"), FireSettlementStatus.TO_INVOICE, SUBCONTRACTOR, 0, PAGE_SIZE);
        assertThat(fireSettlements.countSettlements(query)).isEqualTo(1);
        assertThat(fireSettlements.findSettlements(query)).singleElement().satisfies(listed -> {
            assertThat(listed.shipmentNo()).isEqualTo("SV-L1");
            assertThat(listed.subcontractorTitle()).isEqualTo("Örnek Tekstil");
        });
    }

    @Test
    @DisplayName("Defter yalnız eklemeyi kabul eder: güncelleme ve silme reddedilir")
    void shouldRejectUpdateAndDeleteOfMovements() {
        // given
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);

        // when / then
        assertThatThrownBy(() -> jdbc.update("UPDATE balance_movement SET net_kg = 1 WHERE shipment_id = ?", SHIPMENT))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM balance_movement WHERE shipment_id = ?", SHIPMENT))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Fatura iplik başına INVOICE hareketi olarak bakiyeden düşer; faturalanan kilo toplamlarda görünür")
    void shouldDeductInvoicedKgFromBalance() {
        // given
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);

        // when
        fireInvoices.recordInvoice(new FireInvoicePort.InvoiceRecord(
                SHIPMENT,
                SUBCONTRACTOR,
                "F-1",
                LocalDate.parse("2026-11-12"),
                "ERP",
                List.of(new BalanceMovement.InvoiceShare("(reçetesiz)", new BigDecimal("20"))),
                BigDecimal.ZERO,
                USER,
                AT));

        // then
        assertThat(balanceOf("(reçetesiz)")).isEqualByComparingTo("34012");
        assertThat(fireSettlements.totals(SHIPMENT).invoicedKg()).isEqualByComparingTo("20");
        assertThat(fireInvoices.invoiceExists(SHIPMENT, "F-1")).isTrue();
        assertThat(fireInvoices.invoiceExists(SHIPMENT, "F-2")).isFalse();
        assertThat(fireInvoices.invoices(SHIPMENT)).singleElement().satisfies(entry -> {
            assertThat(entry.invoiceNo()).isEqualTo("F-1");
            assertThat(entry.kg()).isEqualByComparingTo("20");
        });
        assertThat(fireInvoices.dispatchedByYarn(SHIPMENT)).containsKey("(reçetesiz)");
    }

    @Test
    @DisplayName(
            "Faturayla düşülecek itiraz bakiyeyi değiştirmez; faturanın itirazı karşılayan kısmı toplamlarda görünür")
    void shouldCountInvoiceableClaimAndItsInvoicedPart() {
        // given
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);
        UUID discrepancy = UUID.randomUUID();
        ClaimLedgerPort.InvoiceableClaim claim =
                new ClaimLedgerPort.InvoiceableClaim(discrepancy, SHIPMENT, new BigDecimal("100"), AT, USER);

        // when: aynı itiraz iki kez gelse de bir kez yazılır
        assertThat(claims.recordInvoiceableClaim(claim)).isTrue();
        assertThat(claims.recordInvoiceableClaim(claim)).isFalse();

        // then: bakiye değişmez, itiraz fire dosyasında faturalanacak olarak görünür
        assertThat(balanceOf("(reçetesiz)")).isEqualByComparingTo("34032");
        assertThat(fireSettlements.totals(SHIPMENT).claimKg()).isEqualByComparingTo("100");
        assertThat(fireSettlements.totals(SHIPMENT).pendingClaimKg()).isEqualByComparingTo("100");

        // when: 60 kg fatura, hepsi itirazı karşılar
        fireInvoices.recordInvoice(new FireInvoicePort.InvoiceRecord(
                SHIPMENT,
                SUBCONTRACTOR,
                "F-9",
                LocalDate.parse("2026-11-12"),
                null,
                List.of(new BalanceMovement.InvoiceShare("(reçetesiz)", new BigDecimal("60"))),
                new BigDecimal("60"),
                USER,
                AT));

        // then
        assertThat(balanceOf("(reçetesiz)")).isEqualByComparingTo("34032");
        assertThat(fireSettlements.totals(SHIPMENT).claimInvoicedKg()).isEqualByComparingTo("60");
        assertThat(fireSettlements.totals(SHIPMENT).shortageInvoicedKg()).isEqualByComparingTo("0");

        // when: 80 kg fatura, 40 kg itirazı karşılar, 40 kg eksik kilodur
        fireInvoices.recordInvoice(new FireInvoicePort.InvoiceRecord(
                SHIPMENT,
                SUBCONTRACTOR,
                "F-10",
                LocalDate.parse("2026-11-13"),
                null,
                List.of(new BalanceMovement.InvoiceShare("(reçetesiz)", new BigDecimal("80"))),
                new BigDecimal("40"),
                USER,
                AT));

        // then: yalnızca eksik kilo düşer; itiraz kilosu kumaş girişinde zaten düşülmüştür
        assertThat(balanceOf("(reçetesiz)")).isEqualByComparingTo("33992");
        assertThat(fireSettlements.totals(SHIPMENT).shortageInvoicedKg()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("Faturalananlar tarih ve fasoncuya göre süzülür, yeniden eskiye sıralanır, toplam kilo süzgeçten")
    void shouldListInvoicesAcrossShipmentsWithFilters() {
        // given
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);
        for (String[] invoice : new String[][] {{"F-1", "2026-11-10", "20"}, {"F-2", "2026-11-12", "30"}}) {
            fireInvoices.recordInvoice(new FireInvoicePort.InvoiceRecord(
                    SHIPMENT,
                    SUBCONTRACTOR,
                    invoice[0],
                    LocalDate.parse(invoice[1]),
                    null,
                    List.of(new BalanceMovement.InvoiceShare("(reçetesiz)", new BigDecimal(invoice[2]))),
                    BigDecimal.ZERO,
                    USER,
                    AT));
        }
        var all = new ListFireInvoicesQuery(TestTokens.actor("manager"), null, null, null, 0, PAGE_SIZE);
        var november12 = new ListFireInvoicesQuery(
                TestTokens.actor("manager"), LocalDate.parse("2026-11-11"), null, null, 0, PAGE_SIZE);
        var otherSubcontractor =
                new ListFireInvoicesQuery(TestTokens.actor("manager"), null, null, UUID.randomUUID(), 0, PAGE_SIZE);

        // then
        assertThat(invoiceList.findInvoices(all))
                .extracting(row -> row.invoiceNo())
                .containsExactly("F-2", "F-1");
        assertThat(invoiceList.findInvoices(all).getFirst().shipmentNo()).isEqualTo("SV-L1");
        assertThat(invoiceList.countInvoices(all)).isEqualTo(2);
        assertThat(invoiceList.sumInvoiceKg(all)).isEqualByComparingTo("50");
        assertThat(invoiceList.findInvoices(november12))
                .extracting(row -> row.invoiceNo())
                .containsExactly("F-2");
        assertThat(invoiceList.sumInvoiceKg(november12)).isEqualByComparingTo("30");
        assertThat(invoiceList.countInvoices(otherSubcontractor)).isZero();
        assertThat(invoiceList.sumInvoiceKg(otherSubcontractor)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Kapanmış fire dosyası veritabanında da değişmez")
    void shouldRejectUpdateOfInvoicedSettlement() {
        // given
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);
        jdbc.update("UPDATE fire_settlement SET status = 'CLOSED' WHERE shipment_id = ?", SHIPMENT);

        // when / then
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE fire_settlement SET fire_rate = 0.05 WHERE shipment_id = ?", SHIPMENT))
                .isInstanceOf(DataAccessException.class);
    }

    private YarnReturn yarnReturn(ReturnDirection direction, String netKg) {
        return new YarnReturn(
                UUID.randomUUID(),
                SHIPMENT,
                SUBCONTRACTOR,
                direction,
                "30/1 penye",
                null,
                new BigDecimal(netKg),
                "IRS-9",
                AT,
                USER);
    }
}
