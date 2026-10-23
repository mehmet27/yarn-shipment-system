package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase.RemoveBagCommand;
import com.yarnshipment.shipment.application.port.out.ShipmentBagRemovalPort;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
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
    private final List<String> removed = new ArrayList<>();
    private final RecordingBagReservationPort bags = new RecordingBagReservationPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private RemoveBagService service() {
        ShipmentBagRemovalPort port = (shipmentId, uniqueKey, actorUserId, at) -> removed.add(uniqueKey) ? 1 : 0;
        return new RemoveBagService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), port, bags, clock);
    }

    private RemoveBagCommand command() {
        return new RemoveBagCommand(warehouse, TestShipments.SHIPMENT_ID, "BAG-0001");
    }

    @Test
    @DisplayName("Taslak sevkiyattan çıkarılan çuvalın rezervasyonu çuval servisinde bırakılır")
    void shouldRemoveAndRelease() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        service().removeBag(command());

        assertThat(removed).containsExactly("BAG-0001");
        assertThat(bags.releasedKeys()).containsExactly("BAG-0001");
    }

    @Test
    @DisplayName("Çıkıştan sonra çuval çıkarılamaz ve çuval servisine gidilmez")
    void shouldRejectNonDraftShipment() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));
        RemoveBagService service = service();
        RemoveBagCommand command = command();

        assertThatThrownBy(() -> service.removeBag(command)).isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(removed).isEmpty();
        assertThat(bags.releasedKeys()).isEmpty();
    }

    @Test
    @DisplayName("Çuval servisi bırakamazsa istisna fırlar; transaction çıkarmayı geri alır")
    void shouldPropagateReleaseFailure() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        bags.failOnRelease();
        RemoveBagService service = service();
        RemoveBagCommand command = command();

        assertThatThrownBy(() -> service.removeBag(command)).isInstanceOf(BagServiceUnavailableException.class);
    }
}
