package com.yarnshipment.shipment.domain.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CompletionTest {

    private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");
    private static final int TOO_LONG = Completion.MAX_WAYBILL_NO_LENGTH + 1;
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000000ee");

    @Test
    @DisplayName("Teslim bekleyen sevkiyat kırpılmış irsaliye numarası ve belgesiyle tamamlanır")
    void shouldCompleteAwaitingShipment() {
        // when
        Completion completion =
                Completion.of(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY), "  IRS-1  ", EVIDENCE, NOW);

        // then
        assertThat(completion.waybillNo()).isEqualTo("IRS-1");
        assertThat(completion.waybillEvidenceId()).isEqualTo(EVIDENCE);
        assertThat(completion.fromStatus()).isEqualTo(ShipmentStatus.AWAITING_DELIVERY);
        assertThat(completion.toStatus()).isEqualTo(ShipmentStatus.COMPLETED);
        assertThat(ShipmentCompleted.of(completion).completedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "AWAITING_DELIVERY", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Teslim beklemeyen sevkiyat tamamlanamaz")
    void shouldRejectOtherStatuses(ShipmentStatus status) {
        // when / then
        assertThatThrownBy(() -> Completion.of(TestShipments.shipment(status), "IRS-1", EVIDENCE, NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Boş ya da 64 karakterden uzun irsaliye numarası geçersizdir")
    void shouldRejectInvalidWaybillNo() {
        // given
        var shipment = TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY);

        // when / then
        assertThatThrownBy(() -> Completion.of(shipment, "   ", EVIDENCE, NOW))
                .isInstanceOf(InvalidCompletionException.class);
        assertThatThrownBy(() -> Completion.of(shipment, "X".repeat(TOO_LONG), EVIDENCE, NOW))
                .isInstanceOf(InvalidCompletionException.class);
    }
}
