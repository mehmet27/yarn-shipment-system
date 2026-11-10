package com.yarnshipment.shipment.domain.correction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.domain.correction.CorrectedValue.Weight;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorrectionTest {

    private static final UUID ID = UUID.fromString("0192f0a0-0000-7000-8000-00000000c001");
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final Shipment COMPLETED = TestShipments.shipment(ShipmentStatus.COMPLETED);

    private static Correction.Request request(CorrectionTarget target, CorrectedValue value, String reason) {
        return new Correction.Request(target, value, reason);
    }

    private static InvalidCorrectionException rejected(Shipment shipment, Correction.Request request) {
        return (InvalidCorrectionException) catchThrowable(() -> Correction.of(shipment, request, ID, NOW));
    }

    @Test
    @DisplayName("Kilitli sevkiyatın tartımı düzeltilir; eski değer kayıtta kalır, gerekçe kırpılır")
    void shouldCorrectWeighingOfCompletedShipment() {
        Weight corrected = new Weight(new BigDecimal("2010.5"), new BigDecimal("18"));

        Correction correction = Correction.of(
                COMPLETED,
                request(CorrectionTarget.DISPATCH_WEIGHING, corrected, "  Kantar fişi yanlış okunmuş "),
                ID,
                NOW);

        assertThat(correction.oldValue().text()).isEqualTo("gross=2000.500 tare=18.000 net=1982.500");
        assertThat(correction.newValue().text()).isEqualTo("gross=2010.500 tare=18.000 net=1992.500");
        assertThat(correction.reason()).isEqualTo("Kantar fişi yanlış okunmuş");
        assertThat(correction.shipmentId()).isEqualTo(COMPLETED.id());
    }

    @Test
    @DisplayName("Aynı değer, boş gerekçe, girilmemiş kayıt ve iptal edilmiş sevkiyat reddedilir")
    void shouldRejectInvalidCorrections() {
        Weight same = new Weight(new BigDecimal("2000.500"), new BigDecimal("18.000"));
        Weight other = new Weight(new BigDecimal("2001.000"), new BigDecimal("18.000"));

        assertThat(rejected(COMPLETED, request(CorrectionTarget.DISPATCH_WEIGHING, same, "Gerekçe"))
                        .field())
                .isEqualTo("value");
        assertThat(rejected(COMPLETED, request(CorrectionTarget.DISPATCH_WEIGHING, other, "  "))
                        .field())
                .isEqualTo("reason");
        assertThat(rejected(
                                TestShipments.withoutDispatch(ShipmentStatus.DRAFT),
                                request(CorrectionTarget.DISPATCH_WEIGHING, other, "Gerekçe"))
                        .field())
                .isEqualTo("target");
        assertThat(rejected(
                                TestShipments.shipment(ShipmentStatus.CANCELLED),
                                request(CorrectionTarget.DISPATCH_WEIGHING, other, "Gerekçe"))
                        .field())
                .isEqualTo("target");
    }

    @Test
    @DisplayName("Darası brütü aşan ve üç ondalıktan hassas tartım geçersizdir")
    void shouldValidateValues() {
        assertThatThrownBy(() -> new Weight(new BigDecimal("10"), new BigDecimal("11")))
                .isInstanceOf(InvalidCorrectionException.class);
        assertThatThrownBy(() -> new Weight(new BigDecimal("10.0001"), BigDecimal.ONE))
                .isInstanceOf(InvalidCorrectionException.class);
        assertThat(new Weight(new BigDecimal("950"), BigDecimal.ZERO))
                .isEqualTo(new Weight(new BigDecimal("950.000"), new BigDecimal("0.0")));
    }
}
