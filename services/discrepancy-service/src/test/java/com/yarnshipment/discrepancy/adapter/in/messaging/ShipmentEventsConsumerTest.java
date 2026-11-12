package com.yarnshipment.discrepancy.adapter.in.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.events.IncomingEvent;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.RegisterShipmentCommand;
import com.yarnshipment.discrepancy.application.port.in.RegisterShipmentUseCase;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentCommand;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentCommand.Milestone;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentUseCase;
import com.yarnshipment.events.shipment.ShipmentCancelled;
import com.yarnshipment.events.shipment.ShipmentCompleted;
import com.yarnshipment.events.shipment.ShipmentCreated;
import com.yarnshipment.events.shipment.ShipmentDispatched;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentEventsConsumerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");
    private static final UUID SUBCONTRACTOR = TestComparisons.SUBCONTRACTOR;

    private final RegisterShipmentUseCase register = mock(RegisterShipmentUseCase.class);
    private final TrackShipmentUseCase track = mock(TrackShipmentUseCase.class);
    private final ShipmentEventsConsumer consumer = new ShipmentEventsConsumer(register, track);

    @Test
    @DisplayName("Oluşturma olayı sevkiyatı numarası, lokasyonu ve fasoncusuyla izlemeye alır")
    void shouldRegisterCreatedShipment() {
        String created = """
                {"shipmentId":"%s","shipmentNo":"SV-3","siteId":"%s","subcontractorId":"%s",
                 "createdAt":"2026-10-23T09:00:00Z"}
                """.formatted(TestComparisons.SHIPMENT, TestComparisons.SITE, SUBCONTRACTOR);

        consumer.consume(record(ShipmentCreated.TYPE, created, Map.of()));

        verify(register)
                .registerShipment(new RegisterShipmentCommand(
                        TestComparisons.SHIPMENT, "SV-3", TestComparisons.SITE, SUBCONTRACTOR, AT));
        verifyNoInteractions(track);
    }

    @Test
    @DisplayName("Çıkış, tamamlanma ve iptal olayları izleme adımına çevrilir")
    void shouldTrackMilestones() {
        String shipment = TestComparisons.SHIPMENT.toString();
        String site = TestComparisons.SITE.toString();
        String time = "\"2026-10-23T09:00:00Z\"";

        consumer.consume(record(
                ShipmentDispatched.TYPE,
                "{\"shipmentId\":\"%s\",\"siteId\":\"%s\",\"bagCount\":3,\"netKg\":10.5,\"dispatchedAt\":%s}"
                        .formatted(shipment, site, time),
                Map.of()));
        consumer.consume(record(
                ShipmentCompleted.TYPE,
                "{\"shipmentId\":\"%s\",\"siteId\":\"%s\",\"completedAt\":%s}".formatted(shipment, site, time),
                Map.of()));
        consumer.consume(record(
                ShipmentCancelled.TYPE,
                "{\"shipmentId\":\"%s\",\"siteId\":\"%s\",\"cancelledAt\":%s}".formatted(shipment, site, time),
                Map.of()));

        verify(track)
                .trackShipment(new TrackShipmentCommand(
                        TestComparisons.SHIPMENT, Milestone.DISPATCHED, AT, new BigDecimal("10.5")));
        verify(track).trackShipment(new TrackShipmentCommand(TestComparisons.SHIPMENT, Milestone.COMPLETED, AT));
        verify(track).trackShipment(new TrackShipmentCommand(TestComparisons.SHIPMENT, Milestone.CLOSED, AT));
        verifyNoInteractions(register);
    }

    @Test
    @DisplayName("Kullanılmayan sevkiyat olayları hiçbir use case'e gitmez")
    void shouldIgnoreOtherEvents() {
        consumer.consume(record("com.yarnshipment.shipment.shipment-loaded.v1", "{}", Map.of()));

        verifyNoInteractions(register, track);
    }

    private static IncomingEvent record(String type, String payload, Map<String, String> headers) {
        return new IncomingEvent(
                EVENT,
                type,
                TestComparisons.SHIPMENT,
                AT,
                null,
                payload,
                "shipment.events.v1",
                "/yarn-shipment/shipment-service",
                headers);
    }
}
