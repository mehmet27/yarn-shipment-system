package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase.RemoveBagCommand;
import com.yarnshipment.shipment.application.port.out.ShipmentBagRemovalPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RemoveBagServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<Integer> removed = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private RemoveBagService service() {
        ShipmentBagRemovalPort port = (shipmentId, seqNo, actorUserId, at) -> removed.add(seqNo) ? 1 : 0;
        return new RemoveBagService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), port, clock);
    }

    private RemoveBagCommand command() {
        return new RemoveBagCommand(warehouse, TestShipments.SHIPMENT_ID, 1);
    }

    @Test
    @DisplayName("Taslak sevkiyattan çuval sıra numarasıyla çıkarılır")
    void shouldRemoveBag() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        service().removeBag(command());

        assertThat(removed).containsExactly(1);
    }

    @Test
    @DisplayName("Çıkıştan sonra çuval çıkarılamaz")
    void shouldRejectNonDraftShipment() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        RemoveBagService service = service();
        RemoveBagCommand command = command();

        assertThatThrownBy(() -> service.removeBag(command)).isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(removed).isEmpty();
    }
}
