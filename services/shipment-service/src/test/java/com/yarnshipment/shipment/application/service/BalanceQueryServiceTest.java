package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.application.port.out.ActorNamePort;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.DispatchContext;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.DispatchLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BalanceQueryServiceTest {

    private static final UUID KNOWN = UUID.randomUUID();
    private static final UUID UNKNOWN = UUID.randomUUID();
    private static final int PAGE_SIZE = 20;

    @Test
    @DisplayName("Hareket sayfasına, girenlerin okunur adları eklenir; çözülemeyen kişi haritada olmaz")
    void shouldAttachActorNamesToMovementPage() {
        BalanceMovement first = movement(KNOWN);
        BalanceMovement second = movement(UNKNOWN);
        BalanceQueryPort ledger = new BalanceQueryPort() {
            @Override
            public List<com.yarnshipment.shipment.application.port.in.balance.BalanceRow> findBalances(
                    @Nullable UUID subcontractorId) {
                return List.of();
            }

            @Override
            public MovementPage findMovements(ListMovementsQuery query) {
                return new MovementPage(List.of(first, second), 0, PAGE_SIZE, 2);
            }

            @Override
            public Map<String, BigDecimal> findDispatchedByYarn(UUID shipmentId) {
                return Map.of();
            }
        };
        ActorNamePort names = ids -> Map.of(KNOWN, "Veli Müdür");

        MovementPage page = new BalanceQueryService(ledger, names)
                .listBalanceMovements(new ListMovementsQuery(TestTokens.actor("manager"), null, null, 0, PAGE_SIZE));

        assertThat(page.items()).hasSize(2);
        assertThat(page.actorNames()).containsOnlyKeys(KNOWN).containsEntry(KNOWN, "Veli Müdür");
    }

    private static BalanceMovement movement(UUID createdBy) {
        return BalanceMovement.dispatch(
                new DispatchLine(1, "30/1 Penye", null, new BigDecimal("100")),
                new DispatchContext(
                        UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-11-17T08:00:00Z"), createdBy));
    }
}
