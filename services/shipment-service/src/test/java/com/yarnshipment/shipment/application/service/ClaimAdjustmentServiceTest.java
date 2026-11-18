package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort.InvoiceableClaim;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClaimAdjustmentServiceTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000801");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000802");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");

    private final Ledger ledger = new Ledger();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final ClaimAdjustmentService service = new ClaimAdjustmentService(ledger, settlements);

    private RecordClaimAdjustmentCommand command(String kg) {
        return new RecordClaimAdjustmentCommand(UUID.randomUUID(), SHIPMENT, new BigDecimal(kg), AT, USER);
    }

    @Test
    @DisplayName("Açık fire dosyasında haklı itiraz bakiyeden düşülmez, faturalanacak kilo olarak yazılır")
    void shouldRecordInvoiceableClaimForOpenFile() {
        FireSettlement file = FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("1000"))
                .afterFabricReceipt();
        settlements.put(file);

        service.recordClaimAdjustment(command("100.000"));

        assertThat(ledger.invoiceable).containsExactly(new BigDecimal("100.000"));
        assertThat(settlements.find(SHIPMENT)).contains(file);
    }

    @Test
    @DisplayName("Kapanmış ya da faturalanmış dosyaya gelen haklı itiraz dosyayı yeniden faturalanacak yapar")
    void shouldReopenClosedFileForClaim() {
        for (FireSettlementStatus status : List.of(FireSettlementStatus.CLOSED, FireSettlementStatus.INVOICED)) {
            settlements.put(new FireSettlement(
                    SHIPMENT,
                    SUBCONTRACTOR,
                    status,
                    new BigDecimal("0.0400"),
                    new BigDecimal("1000"),
                    "F-1",
                    null,
                    null,
                    null));

            service.recordClaimAdjustment(command("100.000"));

            assertThat(settlements.find(SHIPMENT).orElseThrow().status()).isEqualTo(FireSettlementStatus.TO_INVOICE);
        }
        assertThat(ledger.invoiceable).hasSize(2);
    }

    @Test
    @DisplayName("Fire dosyası olmayan (fasoncusuz) sevkiyatta hiçbir şey yazılmaz")
    void shouldWriteNothingWithoutFile() {
        service.recordClaimAdjustment(command("10.000"));

        assertThat(ledger.invoiceable).isEmpty();
    }

    private static final class Ledger implements ClaimLedgerPort {

        private final List<BigDecimal> invoiceable = new ArrayList<>();

        @Override
        public void recordInvoiceableClaim(InvoiceableClaim claim) {
            invoiceable.add(claim.claimedKg());
        }
    }
}
