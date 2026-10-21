package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.ListShipmentsQuery;
import com.yarnshipment.shipment.application.port.in.ShipmentPage;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListShipmentsServiceTest {

    private static final int PAGE_SIZE = 20;

    private final InMemoryShipmentQueryPort port = new InMemoryShipmentQueryPort();
    private final ScopeGuard scopeGuard =
            new ScopeGuard(event -> {}, Clock.fixed(TestShipments.CREATED_AT, ZoneOffset.UTC));
    private final ShipmentAccessPolicy policy = new ShipmentAccessPolicy();
    private final ListShipmentsService listService = new ListShipmentsService(port, scopeGuard, policy);

    @Test
    @DisplayName("Liste fasoncunun kapsamıyla sorgulanır ve sayfalanır")
    void shouldListWithinActorScope() {
        // given
        Shipment own = TestShipments.shipment(ShipmentStatus.DRAFT);
        port.add(own);
        port.add(new Shipment(
                UUID.randomUUID(),
                "SV-2",
                own.siteId(),
                ShipmentStatus.DRAFT,
                new ShipmentParties(UUID.randomUUID(), "Başka", null, null, null),
                own.planned(),
                own.dispatch(),
                own.delivery(),
                false,
                own.createdAt()));
        Actor actor = TestTokens.CONVERTER.toActor(TestTokens.subcontractorToken());

        // when
        ShipmentPage page = listService.listShipments(new ListShipmentsQuery(actor, Set.of(), 0, PAGE_SIZE));

        // then
        assertThat(port.lastScope().subcontractorId()).isEqualTo(TestShipments.SUBCONTRACTOR_ID);
        assertThat(page.items())
                .singleElement()
                .satisfies(item -> assertThat(item.shipment()).isEqualTo(own));
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Geçersiz sayfa isteği reddedilir")
    void shouldRejectInvalidPageRequest() {
        // given
        Actor actor = TestTokens.CONVERTER.toActor(TestTokens.token("warehouse").build());

        // when / then
        assertThatThrownBy(() -> new ListShipmentsQuery(actor, Set.of(), 0, ListShipmentsQuery.MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ListShipmentsQuery(actor, Set.of(), -1, PAGE_SIZE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
