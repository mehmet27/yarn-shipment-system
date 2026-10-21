package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ShipmentPickupTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");

    @Test
    @DisplayName("Yüklenmiş sevkiyatta beyan çıkış tartısındaki çuval sayısını ve mührü onaylar")
    void shouldConfirmDispatchValues() {
        // given
        Shipment shipment = TestShipments.shipment(ShipmentStatus.LOADED);

        // when
        PickupConfirmation confirmation = shipment.confirmPickup(TestShipments.DRIVER_ID, NOW);

        // then
        assertThat(confirmation.bagCount()).isEqualTo(TestShipments.BAG_COUNT);
        assertThat(confirmation.sealNo()).isEqualTo("M-0001");
        assertThat(confirmation.toStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(ShipmentDispatched.of(confirmation).dispatchedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Çıkış tartısı yoksa beklenen çuval sayısı, o da yoksa sıfır onaylanır")
    void shouldFallBackToPlannedBagCount() {
        // given
        Shipment loaded = TestShipments.shipment(ShipmentStatus.LOADED);
        Shipment withoutWeighing = withDispatchAndPlan(loaded, PlannedLoad.NONE);

        // when / then
        assertThat(withDispatchAndPlan(loaded, loaded.planned())
                        .confirmPickup(TestShipments.DRIVER_ID, NOW)
                        .bagCount())
                .isEqualTo(TestShipments.BAG_COUNT);
        assertThat(withoutWeighing.confirmPickup(TestShipments.DRIVER_ID, NOW).bagCount())
                .isZero();
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "LOADED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Yüklenmiş olmayan sevkiyatta beyan reddedilir")
    void shouldRejectPickupOutsideLoaded(ShipmentStatus status) {
        // given
        Shipment shipment = TestShipments.shipment(status);

        // when / then
        assertThatThrownBy(() -> shipment.confirmPickup(TestShipments.DRIVER_ID, NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    private static Shipment withDispatchAndPlan(Shipment source, PlannedLoad planned) {
        return new Shipment(
                source.id(),
                source.shipmentNo(),
                source.siteId(),
                source.status(),
                source.parties(),
                planned,
                ShipmentSide.EMPTY,
                source.delivery(),
                source.randomCheckSelected(),
                source.createdAt());
    }
}
