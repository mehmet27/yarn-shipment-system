package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.BagAssignmentPort;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignBagsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<BagAssignment> assigned = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private AssignBagsService service() {
        BagAssignmentPort assignments = (assignment, actor, at) -> {
            assigned.add(assignment);
            return assignment.weightsKg().size();
        };
        return new AssignBagsService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), assignments, clock);
    }

    private AssignBagsCommand command() {
        return new AssignBagsCommand(
                warehouse, TestShipments.SHIPMENT_ID, List.of(new BigDecimal("25.000"), new BigDecimal("24.500")));
    }

    @Test
    @DisplayName("Taslak sevkiyata çuvallar kiloyla yazılır")
    void shouldAssignBags() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        int count = service().assignBags(command());

        assertThat(count).isEqualTo(2);
        assertThat(assigned).singleElement().satisfies(assignment -> {
            assertThat(assignment.shipmentId()).isEqualTo(TestShipments.SHIPMENT_ID);
            assertThat(assignment.weightsKg()).containsExactly(new BigDecimal("25.000"), new BigDecimal("24.500"));
        });
    }

    @Test
    @DisplayName("Çıkıştan sonra çuval eklenemez")
    void shouldRejectNonDraftShipment() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        AssignBagsService service = service();
        AssignBagsCommand command = command();

        assertThatThrownBy(() -> service.assignBags(command)).isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(assigned).isEmpty();
    }

    @Test
    @DisplayName("Olmayan sevkiyat 404 verir")
    void shouldFailForMissingShipment() throws Exception {
        AssignBagsService service = service();
        AssignBagsCommand command = command();

        assertThatThrownBy(() -> service.assignBags(command)).isInstanceOf(ShipmentNotFoundException.class);
    }
}
