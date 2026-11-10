package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestCorrections;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.correction.RecordCorrectionCommand;
import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.domain.correction.InvalidCorrectionException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordCorrectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-00000000c002");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor admin = TestTokens.mfaActor("admin");
    private final InMemoryCorrectionPort corrections = new InMemoryCorrectionPort();
    private final ScopedShipmentLoader loader = new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock));
    private final RecordCorrectionService service = new RecordCorrectionService(loader, corrections, clock);

    @BeforeEach
    void addCompletedShipment() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
    }

    private RecordCorrectionCommand command(UUID shipmentId) {
        return new RecordCorrectionCommand(
                admin, shipmentId, KEY, TestCorrections.dispatchWeighing("2010.500", "18.000"));
    }

    @Test
    @DisplayName("Düzeltme tekrar anahtarıyla, yapan Admin adına yazılır ve kaydı döner")
    void shouldRecordCorrection() {
        ShipmentCorrection saved = service.recordCorrection(command(TestShipments.SHIPMENT_ID));

        assertThat(corrections.recorded()).singleElement().satisfies(correction -> {
            assertThat(correction.id()).isEqualTo(KEY);
            assertThat(correction.correctedAt()).isEqualTo(NOW);
        });
        assertThat(saved.oldValue()).isEqualTo("gross=2000.500 tare=18.000 net=1982.500");
        assertThat(saved.requestedBy()).isEqualTo(admin.userId());
        assertThat(corrections.findCorrections(TestShipments.SHIPMENT_ID)).containsExactly(saved);
    }

    @Test
    @DisplayName("Aynı anahtarla tekrar gelen istek düzeltmeyi ikinci kez uygulamaz")
    void shouldNotApplyRepeatedRequestTwice() {
        ShipmentCorrection first = service.recordCorrection(command(TestShipments.SHIPMENT_ID));

        assertThat(service.recordCorrection(command(TestShipments.SHIPMENT_ID))).isEqualTo(first);
        assertThat(corrections.recorded()).hasSize(1);
    }

    @Test
    @DisplayName("Başka sevkiyatın düzeltme anahtarı kullanılamaz")
    void shouldRejectKeyOfAnotherShipment() {
        service.recordCorrection(command(TestShipments.SHIPMENT_ID));
        Shipment base = TestShipments.shipment(ShipmentStatus.COMPLETED);
        UUID other = UUID.randomUUID();
        queryPort.add(new Shipment(
                other,
                "SV-2",
                base.siteId(),
                base.status(),
                base.parties(),
                base.planned(),
                base.dispatchWeighing(),
                null,
                NOW));

        assertThatThrownBy(() -> service.recordCorrection(command(other)))
                .isInstanceOf(InvalidCorrectionException.class);
    }
}
