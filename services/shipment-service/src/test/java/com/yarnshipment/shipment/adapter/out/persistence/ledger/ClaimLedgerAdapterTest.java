package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.service.ClaimAdjustmentService;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
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

/** Haklı itirazın bakiye düzeltmesi gerçek PostgreSQL ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BalanceMovementWriter.class, BalanceLedgerAdapter.class, FireSettlementAdapter.class, ClaimLedgerAdapter.class
})
class ClaimLedgerAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000901");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000902");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-C1', ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', now(), ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                                  created_at, created_by)
            VALUES (?, ?, 'DISPATCH', 34050, 18, 40, now(), ?, now(), ?)""";

    @Autowired
    private BalanceLedgerAdapter ledger;

    @Autowired
    private FireSettlementAdapter fireSettlements;

    @Autowired
    private ClaimLedgerAdapter claims;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertDispatchedShipment() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, TestTokens.SITE_ID, SUBCONTRACTOR, USER);
        jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), SHIPMENT, USER, USER);
        ledger.recordDispatch(SHIPMENT, SUBCONTRACTOR, AT, USER);
        fireSettlements.open(FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("34032")), AT);
    }

    @Test
    @DisplayName("Açık fire dosyasında haklı itiraz bakiyeden düşmez, faturalanacak kilo olur; tekrar etkisizdir")
    void shouldKeepJustifiedClaimAsInvoiceableOnce() {
        ClaimAdjustmentService service = new ClaimAdjustmentService(claims, fireSettlements);
        RecordClaimAdjustmentCommand command =
                new RecordClaimAdjustmentCommand(UUID.randomUUID(), SHIPMENT, new BigDecimal("100.000"), AT, USER);

        service.recordClaimAdjustment(command);
        service.recordClaimAdjustment(command);

        assertThat(ledger.findBalances(SUBCONTRACTOR))
                .singleElement()
                .extracting(BalanceRow::balanceKg)
                .satisfies(kg -> assertThat(kg).isEqualByComparingTo("34032"));
        assertThat(fireSettlements.totals(SHIPMENT).claimKg()).isEqualByComparingTo("100");
        assertThat(fireSettlements.totals(SHIPMENT).pendingClaimKg()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Kapanmış fire dosyasına gelen haklı itiraz dosyayı faturalanacak yapar, bakiye değişmez")
    void shouldReopenClosedFileForClaim() {
        FireSettlement open = fireSettlements.find(SHIPMENT).orElseThrow();
        fireSettlements.save(
                open.afterFabricReceipt()
                        .close(FireCalculation.of(new Inputs(
                                open.sentKg(),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                open.sentKg().multiply(new BigDecimal("0.96")),
                                open.fireRate()))),
                AT);
        ClaimAdjustmentService service = new ClaimAdjustmentService(claims, fireSettlements);

        service.recordClaimAdjustment(
                new RecordClaimAdjustmentCommand(UUID.randomUUID(), SHIPMENT, new BigDecimal("100.000"), AT, USER));

        assertThat(fireSettlements.find(SHIPMENT).orElseThrow().status()).isEqualTo(FireSettlementStatus.TO_INVOICE);
        assertThat(fireSettlements.totals(SHIPMENT).pendingClaimKg()).isEqualByComparingTo("100");
        assertThat(ledger.findBalances(SUBCONTRACTOR))
                .singleElement()
                .extracting(BalanceRow::balanceKg)
                .satisfies(kg -> assertThat(kg).isEqualByComparingTo("34032"));
    }
}
