package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.MovementKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClaimAdjustmentServiceTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000801");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000802");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");

    private final Ledger ledger = new Ledger();
    private final ClaimAdjustmentService service = new ClaimAdjustmentService(ledger, ledger);

    private RecordClaimAdjustmentCommand command(String kg) {
        return new RecordClaimAdjustmentCommand(UUID.randomUUID(), SHIPMENT, new BigDecimal(kg), AT, USER);
    }

    @Test
    @DisplayName("Haklı itiraz kilosu iplik cinslerine gönderim oranıyla dağıtılıp fasoncunun bakiyesinden düşülür")
    void shouldDeductClaimProportionallyFromYarns() {
        ledger.subcontractor = SUBCONTRACTOR;
        ledger.dispatched.put("30/1 Penye", new BigDecimal("600.000"));
        ledger.dispatched.put("20/1 Open End", new BigDecimal("400.000"));

        service.recordClaimAdjustment(command("100.000"));

        assertThat(ledger.appended).hasSize(2);
        assertThat(ledger.appended).allSatisfy(movement -> {
            assertThat(movement.kind()).isEqualTo(MovementKind.CLAIM_ADJUSTMENT);
            assertThat(movement.subcontractorId()).isEqualTo(SUBCONTRACTOR);
            assertThat(movement.shipmentId()).isEqualTo(SHIPMENT);
            assertThat(movement.netKg().signum()).isNegative();
        });
        assertThat(ledger.appended.stream().map(BalanceMovement::netKg))
                .containsExactly(new BigDecimal("-60.000"), new BigDecimal("-40.000"));
    }

    @Test
    @DisplayName("Fasoncusuz ya da hiç gönderilmemiş sevkiyatta düşülecek bakiye olmadığından hareket yazılmaz")
    void shouldWriteNothingWithoutDispatchedBalance() {
        ledger.subcontractor = SUBCONTRACTOR;
        service.recordClaimAdjustment(command("10.000"));

        ledger.subcontractor = null;
        ledger.dispatched.put("30/1 Penye", new BigDecimal("100.000"));
        service.recordClaimAdjustment(command("10.000"));

        assertThat(ledger.appended).isEmpty();
    }

    private static final class Ledger implements ClaimLedgerPort, BalanceQueryPort {

        private @Nullable UUID subcontractor;
        private final Map<String, BigDecimal> dispatched = new LinkedHashMap<>();
        private final List<BalanceMovement> appended = new ArrayList<>();

        @Override
        public Optional<UUID> findSubcontractor(UUID shipmentId) {
            return Optional.ofNullable(subcontractor);
        }

        @Override
        public void append(BalanceMovement movement) {
            appended.add(movement);
        }

        @Override
        public Map<String, BigDecimal> findDispatchedByYarn(UUID shipmentId) {
            return dispatched;
        }

        @Override
        public List<BalanceRow> findBalances(@Nullable UUID subcontractorId) {
            return List.of();
        }

        @Override
        public MovementPage findMovements(ListMovementsQuery query) {
            return new MovementPage(List.of(), 0, 1, 0);
        }
    }
}
