package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentQuery;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentDetails;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetShipmentServiceTest {

    private final InMemoryShipmentQueryPort port = new InMemoryShipmentQueryPort();
    private final List<SecurityAuditEvent> audit = new ArrayList<>();
    private final ScopeGuard scopeGuard =
            new ScopeGuard(audit::add, Clock.fixed(TestShipments.CREATED_AT, ZoneOffset.UTC));
    private final ShipmentAccessPolicy policy = new ShipmentAccessPolicy();
    private final GetShipmentService getService = new GetShipmentService(port, scopeGuard, policy);

    @Test
    @DisplayName("Fasoncu kendi sevkiyatını kendi görünümü ve işlemleriyle alır")
    void shouldReturnOwnShipmentWithSubcontractorAudience() {
        // given
        port.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        Actor actor = TestTokens.CONVERTER.toActor(TestTokens.subcontractorToken());

        // when
        ShipmentDetails details = getService.getShipment(new GetShipmentQuery(actor, TestShipments.SHIPMENT_ID));

        // then
        assertThat(details.audience()).isEqualTo(ShipmentAudience.SUBCONTRACTOR);
        assertThat(details.allowedActions()).containsExactly(ShipmentAction.RECORD_DELIVERY);
    }

    @Test
    @DisplayName("Başka fasoncunun sevkiyatı 403 ile reddedilir ve audit'e yazılır")
    void shouldRejectOtherSubcontractorsShipment() {
        // given
        port.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        Actor other = TestTokens.CONVERTER.toActor(TestTokens.token("subcontractor")
                .claim("subcontractor_id", UUID.randomUUID().toString())
                .build());

        // when / then
        assertThatThrownBy(() -> getService.getShipment(new GetShipmentQuery(other, TestShipments.SHIPMENT_ID)))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThat(audit).extracting(SecurityAuditEvent::reason).containsExactly("OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("Olmayan sevkiyat bulunamadı hatası verir")
    void shouldThrowWhenShipmentDoesNotExist() {
        // given
        Actor actor = TestTokens.CONVERTER.toActor(TestTokens.token("warehouse").build());

        // when / then
        assertThatThrownBy(() -> getService.getShipment(new GetShipmentQuery(actor, UUID.randomUUID())))
                .isInstanceOf(ShipmentNotFoundException.class);
    }
}
