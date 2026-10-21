package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentBagsQuery;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentBags;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kör giriş (SEC-05): görünüme ait olmayan liste hiç okunmaz. */
class GetShipmentBagsServiceTest {

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final StubShipmentBagsPort bagsPort = new StubShipmentBagsPort();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T09:00:00Z"), ZoneOffset.UTC);
    private final GetShipmentBagsService service = new GetShipmentBagsService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            new ShipmentAccessPolicy(),
            bagsPort);

    private ShipmentBags bagsFor(Actor actor) {
        queryPort.add(TestShipments.shipment(ShipmentStatus.CHECKING));
        return service.getBags(new GetShipmentBagsQuery(actor, TestShipments.SHIPMENT_ID));
    }

    @Test
    @DisplayName("Depo yalnızca çıkış çuvallarını alır; teslim okutmaları hiç okunmaz")
    void shouldGiveWarehouseOnlyDispatchBags() {
        ShipmentBags bags = bagsFor(TestTokens.actor("warehouse"));

        assertThat(bags.audience()).isEqualTo(ShipmentAudience.WAREHOUSE);
        assertThat(bags.dispatchBags()).hasSize(1);
        assertThat(bags.deliveryScans()).isNull();
        assertThat(bagsPort.reads()).containsExactly("dispatch");
    }

    @Test
    @DisplayName("Fasoncu yalnızca teslim okutmalarını alır; çıkış çuvalları hiç okunmaz")
    void shouldGiveSubcontractorOnlyDeliveryScans() {
        ShipmentBags bags = bagsFor(TestTokens.CONVERTER.toActor(TestTokens.subcontractorToken()));

        assertThat(bags.dispatchBags()).isNull();
        assertThat(bags.deliveryScans()).hasSize(1);
        assertThat(bagsPort.reads()).containsExactly("delivery");
    }

    @Test
    @DisplayName("Şoför hiçbir liste almaz ve hiçbir liste okunmaz")
    void shouldGiveDriverNothing() {
        ShipmentBags bags = bagsFor(TestTokens.CONVERTER.toActor(TestTokens.driverToken()));

        assertThat(bags.dispatchBags()).isNull();
        assertThat(bags.deliveryScans()).isNull();
        assertThat(bagsPort.reads()).isEmpty();
    }

    @Test
    @DisplayName("İç görünüm iki listeyi de alır")
    void shouldGiveInternalBothLists() {
        ShipmentBags bags = bagsFor(TestTokens.actor("manager"));

        assertThat(bags.dispatchBags()).hasSize(1);
        assertThat(bags.deliveryScans()).hasSize(1);
    }
}
