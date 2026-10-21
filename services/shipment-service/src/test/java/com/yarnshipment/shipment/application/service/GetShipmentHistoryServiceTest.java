package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentHistoryPort;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetShipmentHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final ShipmentHistoryPort historyPort =
            id -> List.of(new StatusChange(null, ShipmentStatus.DRAFT, NOW, UUID.randomUUID(), null));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GetShipmentHistoryService service = new GetShipmentHistoryService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), historyPort);

    @Test
    @DisplayName("Kapsamdaki kullanıcı sevkiyatın geçmişini alır")
    void shouldReturnHistoryInScope() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        List<StatusChange> history = service.getHistory(
                new GetShipmentHistoryQuery(TestTokens.actor("warehouse"), TestShipments.SHIPMENT_ID));

        assertThat(history)
                .singleElement()
                .satisfies(change -> assertThat(change.to()).isEqualTo(ShipmentStatus.DRAFT));
    }

    @Test
    @DisplayName("Olmayan sevkiyatın geçmişi 404, kapsam dışı sevkiyatın geçmişi 403 verir")
    void shouldRejectMissingAndOutOfScopeHistory() {
        Actor stranger = TestTokens.CONVERTER.toActor(TestTokens.token("subcontractor")
                .claim("subcontractor_id", UUID.randomUUID().toString())
                .build());
        GetShipmentHistoryQuery missing =
                new GetShipmentHistoryQuery(TestTokens.actor("warehouse"), TestShipments.SHIPMENT_ID);
        GetShipmentHistoryQuery foreign = new GetShipmentHistoryQuery(stranger, TestShipments.SHIPMENT_ID);

        assertThatThrownBy(() -> service.getHistory(missing)).isInstanceOf(ShipmentNotFoundException.class);
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        assertThatThrownBy(() -> service.getHistory(foreign)).isInstanceOf(OutOfScopeAccessException.class);
    }
}
