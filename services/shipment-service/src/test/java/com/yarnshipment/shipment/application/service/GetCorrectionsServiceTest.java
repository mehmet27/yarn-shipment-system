package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestCorrections;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentHistoryQuery;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetCorrectionsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryCorrectionPort corrections = new InMemoryCorrectionPort();
    private final GetCorrectionsService service = new GetCorrectionsService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, Clock.fixed(NOW, ZoneOffset.UTC))),
            corrections);

    @Test
    @DisplayName("Kapsamdaki sevkiyatın düzeltmeleri okunur; olmayan sevkiyat 404 olur")
    void shouldReadCorrections() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        Correction correction = Correction.of(
                TestShipments.shipment(ShipmentStatus.COMPLETED),
                TestCorrections.sealNo("M-0002"),
                UUID.randomUUID(),
                NOW);
        corrections.recordCorrection(correction, UUID.randomUUID());

        assertThat(service.getCorrections(
                        new GetShipmentHistoryQuery(TestTokens.actor("manager"), TestShipments.SHIPMENT_ID)))
                .singleElement()
                .satisfies(saved -> assertThat(saved.newValue()).isEqualTo("M-0002"));
        assertThatThrownBy(() -> service.getCorrections(
                        new GetShipmentHistoryQuery(TestTokens.actor("manager"), UUID.randomUUID())))
                .isInstanceOf(ShipmentNotFoundException.class);
    }
}
