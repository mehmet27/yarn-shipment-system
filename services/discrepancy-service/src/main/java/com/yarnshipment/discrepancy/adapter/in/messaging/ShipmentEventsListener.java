package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
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
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code shipment.events.v1} dinleyicisi. Oluşturma, çıkış, tamamlanma ve iptal olaylarıyla sevkiyatı zaman ve
 * fotoğraf kuralları için izler. Diğer olaylar kullanılmaz ve onaylanır.
 */
@Component
final class ShipmentEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final RegisterShipmentUseCase registerShipmentUseCase;
    private final TrackShipmentUseCase trackShipmentUseCase;

    ShipmentEventsListener(RegisterShipmentUseCase registerShipmentUseCase, TrackShipmentUseCase trackShipmentUseCase) {
        this.registerShipmentUseCase = registerShipmentUseCase;
        this.trackShipmentUseCase = trackShipmentUseCase;
    }

    @KafkaListener(topics = ShipmentEvents.TOPIC)
    void onShipmentEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        switch (event.type()) {
            case ShipmentCreated.TYPE -> onCreated(event);
            case ShipmentDispatched.TYPE -> {
                ShipmentDispatched dispatched = EVENT_READER.readValue(event.data(), ShipmentDispatched.class);
                track(dispatched.shipmentId(), Milestone.DISPATCHED, dispatched.dispatchedAt());
            }
            case ShipmentCompleted.TYPE -> {
                ShipmentCompleted completed = EVENT_READER.readValue(event.data(), ShipmentCompleted.class);
                track(completed.shipmentId(), Milestone.CLOSED, completed.completedAt());
            }
            case ShipmentCancelled.TYPE -> {
                ShipmentCancelled cancelled = EVENT_READER.readValue(event.data(), ShipmentCancelled.class);
                track(cancelled.shipmentId(), Milestone.CLOSED, cancelled.cancelledAt());
            }
            default -> {
                // Bu servisin kullanmadığı sevkiyat olayı.
            }
        }
    }

    private void onCreated(IncomingEvent event) {
        ShipmentCreated created = EVENT_READER.readValue(event.data(), ShipmentCreated.class);
        registerShipmentUseCase.registerShipment(new RegisterShipmentCommand(
                created.shipmentId(),
                created.shipmentNo(),
                created.siteId(),
                created.subcontractorId(),
                created.createdAt()));
    }

    private void track(UUID shipmentId, Milestone milestone, Instant at) {
        trackShipmentUseCase.trackShipment(new TrackShipmentCommand(shipmentId, milestone, at));
    }
}
