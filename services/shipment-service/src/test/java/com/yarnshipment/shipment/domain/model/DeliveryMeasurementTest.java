package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestDeliveries;
import com.yarnshipment.shipment.TestShipments;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeliveryMeasurementTest {

    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");
    private static final BigDecimal TEN = new BigDecimal("10.000");

    @Test
    @DisplayName("Teslim beklenen sevkiyatta tutanak teslim tartısını sunucu saatiyle kurar")
    void shouldReceiveDeliveryWhenAwaiting() {
        // given
        Shipment shipment = TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY);

        // when
        DeliveryReceipt receipt =
                shipment.receiveDelivery(TestShipments.SUBCONTRACTOR_ID, TestDeliveries.measurement(), NOW);

        // then
        assertThat(receipt.weighing().netKg()).isEqualByComparingTo("1977.250");
        assertThat(receipt.weighing().weighedAt()).isEqualTo(NOW);
        assertThat(receipt.toStatus()).isEqualTo(ShipmentStatus.CHECKING);
    }

    @Test
    @DisplayName("Olay anahtarları sıralı taşır, çıkış mührünün durumunu boş bırakır")
    void shouldBuildDeliveryReceivedEvent() {
        // given
        DeliveryReceipt receipt = TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY)
                .receiveDelivery(TestShipments.SUBCONTRACTOR_ID, TestDeliveries.measurement(), NOW);

        // when
        DeliveryReceived event = DeliveryReceived.of(receipt, TestShipments.SHIPMENT_ID, List.of("UK-0002", "UK-0001"));

        // then
        assertThat(event.expectedUniqueKeys()).containsExactly("UK-0001", "UK-0002");
        assertThat(event.scannedUniqueKeys()).containsExactly("UK-0001", "UK-9999");
        assertThat(event.dispatch()).isNotNull();
        assertThat(event.dispatchSeal().condition()).isNull();
        assertThat(event.deliverySeal().condition()).isEqualTo(SealCondition.INTACT);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "AWAITING_DELIVERY", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Teslim beklenmeyen sevkiyatta tutanak reddedilir")
    void shouldRejectDeliveryOutsideAwaiting(ShipmentStatus status) {
        // given
        Shipment shipment = TestShipments.shipment(status);

        // when / then
        assertThatThrownBy(() ->
                        shipment.receiveDelivery(TestShipments.SUBCONTRACTOR_ID, TestDeliveries.measurement(), NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Dara brütten büyükse giriş reddedilir")
    void shouldRejectTareAboveGross() {
        assertThatThrownBy(() -> weights(TEN, new BigDecimal("11.000"), 1))
                .isInstanceOf(InvalidDeliveryException.class)
                .extracting(exception -> ((InvalidDeliveryException) exception).field())
                .isEqualTo("tareKg");
    }

    @Test
    @DisplayName("Çuval sayısı sıfırsa giriş reddedilir")
    void shouldRejectZeroBags() {
        assertThatThrownBy(() -> weights(TEN, TEN, 0)).isInstanceOf(InvalidDeliveryException.class);
    }

    @Test
    @DisplayName("Mühür numarası yalnızca mühür yoksa boş bırakılabilir")
    void shouldRequireSealNoUnlessMissing() {
        assertThatThrownBy(() -> seal(null, SealCondition.DAMAGED)).isInstanceOf(InvalidDeliveryException.class);
        assertThat(seal(null, SealCondition.MISSING).sealNo()).isNull();
    }

    private static DeliveryMeasurement weights(BigDecimal gross, BigDecimal tare, int bags) {
        return new DeliveryMeasurement(
                gross, tare, bags, SealCondition.INTACT, "M-1", Set.of(), ScanMethod.MANUAL, DeliveryDocument.NONE);
    }

    private static DeliveryMeasurement seal(@Nullable String sealNo, SealCondition condition) {
        return new DeliveryMeasurement(
                TEN, TEN, 1, condition, sealNo, Set.of(), ScanMethod.MANUAL, DeliveryDocument.NONE);
    }
}
