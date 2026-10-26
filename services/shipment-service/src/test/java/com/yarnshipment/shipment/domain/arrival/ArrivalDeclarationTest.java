package com.yarnshipment.shipment.domain.arrival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ArrivalDeclarationTest {

    private static final Instant NOW = Instant.parse("2026-10-21T13:00:00Z");

    @Test
    @DisplayName("Yoldaki sevkiyatta varış beyanı mühür durumunu ve çıkış mührünü taşır")
    void shouldDeclareArrival() {
        // given
        Shipment shipment = TestShipments.shipment(ShipmentStatus.IN_TRANSIT);

        // when
        ArrivalDeclaration declaration = ArrivalDeclaration.of(shipment, TestShipments.DRIVER_ID, false, NOW);

        // then
        assertThat(declaration.sealIntact()).isFalse();
        assertThat(declaration.sealNo()).isEqualTo("M-0001");
        assertThat(declaration.bagCount()).isEqualTo(TestShipments.BAG_COUNT);
        assertThat(declaration.toStatus()).isEqualTo(ShipmentStatus.AWAITING_DELIVERY);
        assertThat(ShipmentArrived.of(declaration).arrivedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "IN_TRANSIT", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Yolda olmayan sevkiyatta varış beyanı reddedilir")
    void shouldRejectArrivalOutsideInTransit(ShipmentStatus status) {
        // given
        Shipment shipment = TestShipments.shipment(status);

        // when / then
        assertThatThrownBy(() -> ArrivalDeclaration.of(shipment, TestShipments.DRIVER_ID, true, NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }
}
