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
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        return service.getBags(new GetShipmentBagsQuery(actor, TestShipments.SHIPMENT_ID));
    }

    @Test
    @DisplayName("Depo çıkış çuvallarını kendi görünümüyle alır")
    void shouldGiveWarehouseDispatchBags() {
        ShipmentBags bags = bagsFor(TestTokens.actor("warehouse"));

        assertThat(bags.audience()).isEqualTo(ShipmentAudience.WAREHOUSE);
        assertThat(bags.dispatchBags()).hasSize(1);
        assertThat(bagsPort.reads()).containsExactly("dispatch");
    }

    @Test
    @DisplayName("İç görünüm çıkış çuvallarını alır")
    void shouldGiveInternalDispatchBags() {
        ShipmentBags bags = bagsFor(TestTokens.actor("manager"));

        assertThat(bags.audience()).isEqualTo(ShipmentAudience.INTERNAL);
        assertThat(bags.dispatchBags()).hasSize(1);
    }
}
