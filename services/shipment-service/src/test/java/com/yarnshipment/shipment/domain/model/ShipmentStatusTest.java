package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShipmentStatusTest {

    @ParameterizedTest
    @CsvSource({
        "DRAFT, AWAITING_DELIVERY",
        "DRAFT, CANCELLED",
        "DRAFT, WEIGHT_MISMATCH",
        "WEIGHT_MISMATCH, DRAFT",
        "WEIGHT_MISMATCH, AWAITING_DELIVERY",
        "WEIGHT_MISMATCH, CANCELLED",
        "AWAITING_DELIVERY, COMPLETED",
        "AWAITING_DELIVERY, CANCELLED"
    })
    @DisplayName("Saha analizindeki geçişlere izin verilir")
    void shouldAllowDocumentedTransitions(ShipmentStatus from, ShipmentStatus to) {
        // when / then
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT, COMPLETED",
        "WEIGHT_MISMATCH, COMPLETED",
        "AWAITING_DELIVERY, WEIGHT_MISMATCH",
        "AWAITING_DELIVERY, DRAFT",
        "COMPLETED, CANCELLED",
        "CANCELLED, DRAFT"
    })
    @DisplayName("Tanımsız geçiş reddedilir")
    void shouldRejectUndocumentedTransitions(ShipmentStatus from, ShipmentStatus to) {
        // when / then
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    @DisplayName("Tamamlanan ve iptal edilen sevkiyatın sonraki durumu yoktur")
    void shouldHaveNoNextStatusForFinalStatuses() {
        // when / then
        assertThat(ShipmentStatus.COMPLETED.nextStatuses()).isEmpty();
        assertThat(ShipmentStatus.CANCELLED.nextStatuses()).isEmpty();
    }
}
