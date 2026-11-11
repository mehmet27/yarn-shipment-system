package com.yarnshipment.discrepancy.adapter.in.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.OutboxEvent;
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
import com.yarnshipment.events.shipment.ShipmentEvents;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentEventsListenerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");
    private static final UUID SUBCONTRACTOR = TestComparisons.SUBCONTRACTOR;

    private final RegisterShipmentUseCase register = mock(RegisterShipmentUseCase.class);
    private final TrackShipmentUseCase track = mock(TrackShipmentUseCase.class);
    private final ShipmentEventsListener listener = new ShipmentEventsListener(register, track);

    @Test
    @DisplayName("Oluşturma olayı sevkiyatı numarası, lokasyonu ve fasoncusuyla izlemeye alır")
    void shouldRegisterCreatedShipment() {
        String created = """
                {"shipmentId":"%s","shipmentNo":"SV-3","siteId":"%s","subcontractorId":"%s",
                 "createdAt":"2026-10-23T09:00:00Z"}
                """.formatted(TestComparisons.SHIPMENT, TestComparisons.SITE, SUBCONTRACTOR);

        listener.onShipmentEvent(record(ShipmentCreated.TYPE, created, Map.of()));

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

        listener.onShipmentEvent(record(
                ShipmentDispatched.TYPE,
                "{\"shipmentId\":\"%s\",\"siteId\":\"%s\",\"bagCount\":3,\"netKg\":10.5,\"dispatchedAt\":%s}"
                        .formatted(shipment, site, time),
                Map.of()));
        listener.onShipmentEvent(record(
                ShipmentCompleted.TYPE,
                "{\"shipmentId\":\"%s\",\"siteId\":\"%s\",\"completedAt\":%s}".formatted(shipment, site, time),
                Map.of()));
        listener.onShipmentEvent(record(
                ShipmentCancelled.TYPE,
                "{\"shipmentId\":\"%s\",\"siteId\":\"%s\",\"cancelledAt\":%s}".formatted(shipment, site, time),
                Map.of()));

        verify(track).trackShipment(new TrackShipmentCommand(TestComparisons.SHIPMENT, Milestone.DISPATCHED, AT));
        verify(track, times(2)).trackShipment(new TrackShipmentCommand(TestComparisons.SHIPMENT, Milestone.CLOSED, AT));
        verifyNoInteractions(register);
    }

    @Test
    @DisplayName("Kullanılmayan sevkiyat olayları hiçbir use case'e gitmez")
    void shouldIgnoreOtherEvents() {
        listener.onShipmentEvent(record("com.yarnshipment.shipment.shipment-loaded.v1", "{}", Map.of()));

        verifyNoInteractions(register, track);
    }

    private static ConsumerRecord<String, String> record(String type, String payload, Map<String, String> headers) {
        OutboxEvent event = new OutboxEvent(EVENT, AT, TestComparisons.SHIPMENT, type, payload, headers, AT);
        ProducerRecord<String, String> produced =
                CloudEventHeaders.toRecord(ShipmentEvents.TOPIC, "/yarn-shipment/shipment-service", event);
        ConsumerRecord<String, String> consumed =
                new ConsumerRecord<>(ShipmentEvents.TOPIC, 0, 0L, produced.key(), produced.value());
        for (Header header : produced.headers()) {
            consumed.headers().add(header);
        }
        return consumed;
    }
}
