package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeightCheckTest {

    private static final int BAG_COUNT = 40;
    private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");

    private static DispatchMeasurement measurement(String gross, String tare) {
        return new DispatchMeasurement(new BigDecimal(gross), new BigDecimal(tare), BAG_COUNT);
    }

    @Test
    @DisplayName("Fark işaretli hesaplanır; eksik yükte negatif, tolerans sınırı dahildir")
    void shouldComputeSignedDifference() {
        WeightCheck check = new WeightCheck(
                new BigDecimal("18.000"),
                new BigDecimal("1900.000"),
                new BigDecimal("1982.500"),
                new BigDecimal("100.500"),
                null);

        assertThat(check.measuredKg()).isEqualByComparingTo("1882.000");
        assertThat(check.diffKg()).isEqualByComparingTo("-100.500");
        assertThat(check.exceedsTolerance()).isFalse();
    }

    @Test
    @DisplayName("Tolerans 0 iken en küçük fark bile sevkiyatı uyuşmazlığa düşürür")
    void shouldFlagAnyDifferenceAtZeroTolerance() {
        DispatchRecording recording = TestShipments.shipment(ShipmentStatus.DRAFT)
                .recordDispatch(measurement("2000.501", "18.000"), NOW, BigDecimal.ZERO);

        assertThat(recording.toStatus()).isEqualTo(ShipmentStatus.WEIGHT_MISMATCH);
        assertThat(Objects.requireNonNull(recording.mismatch()).diffKg()).isEqualByComparingTo("0.001");
    }

    @Test
    @DisplayName("Tam eşleşen tartı yola çıkar")
    void shouldDispatchOnExactMatch() {
        DispatchRecording recording = TestShipments.shipment(ShipmentStatus.DRAFT)
                .recordDispatch(measurement("2000.500", "18.000"), NOW, BigDecimal.ZERO);

        assertThat(recording.toStatus()).isEqualTo(ShipmentStatus.AWAITING_DELIVERY);
    }

    @Test
    @DisplayName("Beklenen net bilinmiyorsa karşılaştırma yapılmaz")
    void shouldSkipCheckWithoutPlannedNet() {
        Shipment unplanned = new Shipment(
                TestShipments.SHIPMENT_ID,
                "SV-1",
                TestShipments.SITE_ID,
                ShipmentStatus.DRAFT,
                TestShipments.shipment(ShipmentStatus.DRAFT).parties(),
                PlannedLoad.NONE,
                null,
                null,
                NOW,
                Shipment.RecipeState.LEGACY);

        assertThat(unplanned
                        .recordDispatch(measurement("500.000", "18.000"), NOW, BigDecimal.ZERO)
                        .mismatch())
                .isNull();
    }

    @Test
    @DisplayName("Uyuşmazlıktaki sevkiyat yeniden tartılamaz; önce müdür karar vermelidir")
    void shouldRejectDispatchWhileMismatched() {
        assertThatThrownBy(() -> TestShipments.shipment(ShipmentStatus.WEIGHT_MISMATCH)
                        .recordDispatch(measurement("2000.500", "18.000"), NOW, BigDecimal.ZERO))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }
}
