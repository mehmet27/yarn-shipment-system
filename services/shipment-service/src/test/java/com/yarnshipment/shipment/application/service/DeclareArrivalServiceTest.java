package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalCommand;
import com.yarnshipment.shipment.domain.arrival.ArrivalDeclaration;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeclareArrivalServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T13:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cd");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final RecordingShipmentCommandPort commandPort = new RecordingShipmentCommandPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final DeclareArrivalService service = new DeclareArrivalService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), commandPort, clock);
    private final Actor driver = TestTokens.CONVERTER.toActor(TestTokens.driverToken());

    @Test
    @DisplayName("Atanmış şoför yoldaki sevkiyatın varışını beyan eder; beyan sunucu saatiyle kaydedilir")
    void shouldRecordArrivalForAssignedDriver() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.IN_TRANSIT));

        // when
        service.declareArrival(new DeclareArrivalCommand(driver, TestShipments.SHIPMENT_ID, KEY, false));

        // then
        assertThat(commandPort.arrivals()).singleElement().satisfies(arrival -> {
            assertThat(arrival.driverId()).isEqualTo(TestShipments.DRIVER_ID);
            assertThat(arrival.sealIntact()).isFalse();
            assertThat(arrival.declaredAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("Aynı beyanın tekrarı yeni kayıt oluşturmaz ve hata vermez")
    void shouldIgnoreRepeatedArrival() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        commandPort.addExistingArrival(ArrivalDeclaration.of(
                TestShipments.shipment(ShipmentStatus.IN_TRANSIT), TestShipments.DRIVER_ID, true, NOW));

        // when
        service.declareArrival(new DeclareArrivalCommand(driver, TestShipments.SHIPMENT_ID, KEY, true));

        // then
        assertThat(commandPort.arrivals()).hasSize(1);
    }

    @Test
    @DisplayName("Beyanı olmayan ve yolda olmayan sevkiyatta 409 hatası verilir")
    void shouldRejectArrivalBeforePickup() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));

        // when / then
        assertThatThrownBy(() ->
                        service.declareArrival(new DeclareArrivalCommand(driver, TestShipments.SHIPMENT_ID, KEY, true)))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(commandPort.arrivals()).isEmpty();
    }

    @Test
    @DisplayName("Başka şoförün sevkiyatı kapsam dışıdır")
    void shouldRejectOtherDriver() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.IN_TRANSIT));
        Actor other = TestTokens.CONVERTER.toActor(TestTokens.token("driver")
                .claim("driver_id", UUID.randomUUID().toString())
                .build());

        // when / then
        assertThatThrownBy(() ->
                        service.declareArrival(new DeclareArrivalCommand(other, TestShipments.SHIPMENT_ID, KEY, true)))
                .isInstanceOf(OutOfScopeAccessException.class);
    }
}
