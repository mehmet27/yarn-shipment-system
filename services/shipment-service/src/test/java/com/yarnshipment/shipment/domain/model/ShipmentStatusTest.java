package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShipmentStatusTest {

    @ParameterizedTest
    @CsvSource({
        "DRAFT, LOADED",
        "DRAFT, CANCELLED",
        "LOADED, IN_TRANSIT",
        "LOADED, CANCELLED",
        "IN_TRANSIT, AWAITING_DELIVERY",
        "AWAITING_DELIVERY, CHECKING",
        "CHECKING, COMPLETED",
        "CHECKING, IN_REVIEW",
        "IN_REVIEW, COMPLETED"
    })
    @DisplayName("Rapor 7.2'deki geçişlere izin verilir")
    void shouldAllowDocumentedTransitions(ShipmentStatus from, ShipmentStatus to) {
        // when / then
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"IN_TRANSIT, CANCELLED", "AWAITING_DELIVERY, IN_TRANSIT", "DRAFT, COMPLETED", "IN_REVIEW, CHECKING"})
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
