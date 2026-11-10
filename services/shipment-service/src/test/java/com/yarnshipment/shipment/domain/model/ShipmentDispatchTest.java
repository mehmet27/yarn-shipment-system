package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ShipmentDispatchTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final int BAGS = 40;

    private static DispatchMeasurement measurement() {
        return new DispatchMeasurement(new BigDecimal("2000.500"), new BigDecimal("18.000"), BAGS, "KF-1");
    }

    @Test
    @DisplayName("Taslak sevkiyatta çıkış kaydı tartımı taşır, durum AWAITING_DELIVERY olur")
    void shouldRecordDispatchForDraft() {
        DispatchRecording recording =
                TestShipments.shipment(ShipmentStatus.DRAFT).recordDispatch(measurement(), NOW);

        assertThat(recording.fromStatus()).isEqualTo(ShipmentStatus.DRAFT);
        assertThat(recording.toStatus()).isEqualTo(ShipmentStatus.AWAITING_DELIVERY);
        assertThat(recording.weighing().netKg()).isEqualByComparingTo("1982.500");
        assertThat(recording.weighing().bagCount()).isEqualTo(BAGS);
        assertThat(recording.scaleTicketNo()).isEqualTo("KF-1");
        assertThat(recording.recordedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Çıkış olayı fasoncuyu, çuval sayısını ve net kiloyu taşır")
    void shouldPublishDispatchFigures() {
        DispatchRecording recording =
                TestShipments.shipment(ShipmentStatus.DRAFT).recordDispatch(measurement(), NOW);

        ShipmentDispatched event = ShipmentDispatched.of(recording);

        assertThat(event.subcontractorId()).isEqualTo(TestShipments.SUBCONTRACTOR_ID);
        assertThat(event.bagCount()).isEqualTo(BAGS);
        assertThat(event.netKg()).isEqualByComparingTo("1982.500");
        assertThat(event.dispatchedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Taslak olmayan sevkiyatta çıkış kaydı reddedilir")
    void shouldRejectDispatchOutsideDraft(ShipmentStatus status) {
        Shipment shipment = TestShipments.shipment(status);

        assertThatThrownBy(() -> shipment.recordDispatch(measurement(), NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Dara brütten büyükse veya negatifse girdi reddedilir")
    void shouldRejectInvalidTare() {
        assertThatThrownBy(() -> new DispatchMeasurement(BigDecimal.ONE, BigDecimal.TEN, BAGS, null))
                .isInstanceOfSatisfying(
                        InvalidDispatchException.class,
                        e -> assertThat(e.field()).isEqualTo("tareKg"));
        assertThatThrownBy(() -> new DispatchMeasurement(BigDecimal.ONE, BigDecimal.valueOf(-1), BAGS, null))
                .isInstanceOf(InvalidDispatchException.class);
    }

    @Test
    @DisplayName("Çuval sayısı pozitif olmalı")
    void shouldRejectInvalidBagCount() {
        assertThatThrownBy(() -> new DispatchMeasurement(BigDecimal.TEN, BigDecimal.ONE, 0, null))
                .isInstanceOfSatisfying(
                        InvalidDispatchException.class,
                        e -> assertThat(e.field()).isEqualTo("bagCount"));
    }
}
