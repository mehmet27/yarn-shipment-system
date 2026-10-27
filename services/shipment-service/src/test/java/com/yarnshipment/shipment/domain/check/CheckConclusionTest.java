package com.yarnshipment.shipment.domain.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckConclusionTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");

    @Test
    @DisplayName("Temiz karşılaştırma sevkiyatı tamamlar ve tamamlanma olayı lokasyonu taşır")
    void shouldCompleteWhenClean() {
        CheckConclusion conclusion =
                CheckConclusion.of(TestShipments.shipment(ShipmentStatus.CHECKING), ComparisonOutcome.CLEAN, NOW);

        assertThat(conclusion.fromStatus()).isEqualTo(ShipmentStatus.CHECKING);
        assertThat(conclusion.toStatus()).isEqualTo(ShipmentStatus.COMPLETED);
        assertThat(conclusion.completed()).isTrue();
        assertThat(ShipmentCompleted.of(conclusion))
                .isEqualTo(new ShipmentCompleted(TestShipments.SHIPMENT_ID, TestShipments.SITE_ID, NOW));
    }

    @Test
    @DisplayName("Uyuşmazlık varsa sevkiyat incelemeye alınır")
    void shouldReviewWhenDiscrepancy() {
        CheckConclusion conclusion =
                CheckConclusion.of(TestShipments.shipment(ShipmentStatus.CHECKING), ComparisonOutcome.DISCREPANCY, NOW);

        assertThat(conclusion.toStatus()).isEqualTo(ShipmentStatus.IN_REVIEW);
        assertThat(conclusion.completed()).isFalse();
    }

    @Test
    @DisplayName("Kontrolde olmayan sevkiyat sonuçlandırılamaz")
    void shouldRejectOutsideChecking() {
        assertThatThrownBy(() -> CheckConclusion.of(
                        TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY), ComparisonOutcome.CLEAN, NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("İnceleme bitince sevkiyat IN_REVIEW'dan COMPLETED'a geçer; başka durumdan geçemez")
    void shouldCompleteAfterReview() {
        CheckConclusion conclusion = CheckConclusion.afterReview(TestShipments.shipment(ShipmentStatus.IN_REVIEW), NOW);

        assertThat(conclusion.fromStatus()).isEqualTo(ShipmentStatus.IN_REVIEW);
        assertThat(conclusion.toStatus()).isEqualTo(ShipmentStatus.COMPLETED);
        assertThatThrownBy(() -> CheckConclusion.afterReview(TestShipments.shipment(ShipmentStatus.CHECKING), NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }
}
