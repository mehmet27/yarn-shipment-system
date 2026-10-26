package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.in.driver.ConfirmPickupCommand;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfirmPickupServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final RecordingShipmentCommandPort commandPort = new RecordingShipmentCommandPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ConfirmPickupService service = new ConfirmPickupService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), commandPort, clock);
    private final Actor driver = TestTokens.CONVERTER.toActor(TestTokens.driverToken());

    @Test
    @DisplayName("Atanmış şoför yüklenmiş sevkiyatı teslim alır; beyan sunucu saatiyle kaydedilir")
    void shouldRecordPickupForAssignedDriver() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));

        // when
        service.confirmPickup(new ConfirmPickupCommand(driver, TestShipments.SHIPMENT_ID, KEY));

        // then
        assertThat(commandPort.pickups()).singleElement().satisfies(pickup -> {
            assertThat(pickup.driverId()).isEqualTo(TestShipments.DRIVER_ID);
            assertThat(pickup.declaredAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("Aynı beyanın tekrarı yeni kayıt oluşturmaz ve hata vermez")
    void shouldIgnoreRepeatedPickup() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.IN_TRANSIT));
        commandPort.addExistingPickup(
                TestShipments.shipment(ShipmentStatus.LOADED).confirmPickup(TestShipments.DRIVER_ID, NOW));

        // when
        service.confirmPickup(new ConfirmPickupCommand(driver, TestShipments.SHIPMENT_ID, KEY));

        // then
        assertThat(commandPort.pickups()).hasSize(1);
    }

    @Test
    @DisplayName("Beyanı olmayan ve yüklenmiş olmayan sevkiyatta 409 hatası verilir")
    void shouldRejectPickupInDraft() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        // when / then
        assertThatThrownBy(
                        () -> service.confirmPickup(new ConfirmPickupCommand(driver, TestShipments.SHIPMENT_ID, KEY)))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(commandPort.pickups()).isEmpty();
    }

    @Test
    @DisplayName("Başka şoförün sevkiyatı kapsam dışıdır")
    void shouldRejectOtherDriver() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));
        Actor other = TestTokens.CONVERTER.toActor(TestTokens.token("driver")
                .claim("driver_id", UUID.randomUUID().toString())
                .build());

        // when / then
        assertThatThrownBy(() -> service.confirmPickup(new ConfirmPickupCommand(other, TestShipments.SHIPMENT_ID, KEY)))
                .isInstanceOf(OutOfScopeAccessException.class);
    }

    @Test
    @DisplayName("Olmayan sevkiyat bulunamadı hatası verir")
    void shouldThrowWhenShipmentMissing() {
        // when / then
        assertThatThrownBy(() -> service.confirmPickup(new ConfirmPickupCommand(driver, UUID.randomUUID(), KEY)))
                .isInstanceOf(ShipmentNotFoundException.class);
    }
}
