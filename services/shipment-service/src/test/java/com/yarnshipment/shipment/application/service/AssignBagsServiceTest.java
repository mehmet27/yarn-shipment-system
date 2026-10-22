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
import com.yarnshipment.shipment.application.port.out.BagReservationPort;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.bag.BagReservationException;
import com.yarnshipment.shipment.domain.bag.BagReservationException.Reason;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignBagsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    private static final ReservedBag BAG = new ReservedBag(UUID.randomUUID(), "BAG-0001", "P-2026-0001", "A");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<BagAssignment> assigned = new ArrayList<>();
    private final List<String> reservedKeys = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private AssignBagsService service(BagReservationPort reservations) {
        BagAssignmentPort assignments = (assignment, actor, at) -> {
            assigned.add(assignment);
            return assignment.bags().size();
        };
        return new AssignBagsService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
                reservations,
                assignments,
                clock);
    }

    private BagReservationPort reservingBag() {
        return (shipmentId, keys) -> {
            reservedKeys.addAll(keys);
            return List.of(BAG);
        };
    }

    private AssignBagsCommand command() {
        return new AssignBagsCommand(warehouse, TestShipments.SHIPMENT_ID, List.of("BAG-0001"), ScanMethod.SCAN);
    }

    @Test
    @DisplayName("Taslak sevkiyatın çuvalları ayrılır ve sevkiyata yazılır")
    void shouldReserveAndAssignBags() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        int count = service(reservingBag()).assignBags(command());

        assertThat(count).isEqualTo(1);
        assertThat(reservedKeys).containsExactly("BAG-0001");
        assertThat(assigned).singleElement().satisfies(assignment -> {
            assertThat(assignment.shipmentId()).isEqualTo(TestShipments.SHIPMENT_ID);
            assertThat(assignment.scanMethod()).isEqualTo(ScanMethod.SCAN);
        });
    }

    @Test
    @DisplayName("Çıkıştan sonra çuval atanamaz ve çuval servisine hiç gidilmez")
    void shouldRejectNonDraftShipment() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));
        AssignBagsService service = service(reservingBag());
        AssignBagsCommand command = command();

        assertThatThrownBy(() -> service.assignBags(command)).isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(reservedKeys).isEmpty();
    }

    @Test
    @DisplayName("Çuval servisi reddederse hiçbir çuval sevkiyata yazılmaz")
    void shouldWriteNothingWhenReservationIsRejected() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        BagReservationPort rejecting = (shipmentId, keys) -> {
            throw new BagReservationException(Reason.ALREADY_RESERVED);
        };
        AssignBagsService service = service(rejecting);
        AssignBagsCommand command = command();

        assertThatThrownBy(() -> service.assignBags(command)).isInstanceOf(BagReservationException.class);
        assertThat(assigned).isEmpty();
    }

    @Test
    @DisplayName("Olmayan sevkiyat 404 verir")
    void shouldFailForMissingShipment() throws Exception {
        AssignBagsService service = service(reservingBag());
        AssignBagsCommand command = command();

        assertThatThrownBy(() -> service.assignBags(command)).isInstanceOf(ShipmentNotFoundException.class);
    }
}
