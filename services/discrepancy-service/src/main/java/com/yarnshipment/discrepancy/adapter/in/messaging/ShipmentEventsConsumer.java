package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.events.DomainEventConsumer;
import com.yarnshipment.common.events.IncomingEvent;
import com.yarnshipment.discrepancy.application.port.in.RegisterShipmentCommand;
import com.yarnshipment.discrepancy.application.port.in.RegisterShipmentUseCase;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentCommand;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentCommand.Milestone;
import com.yarnshipment.discrepancy.application.port.in.TrackShipmentUseCase;
import com.yarnshipment.events.shipment.ShipmentCancelled;
import com.yarnshipment.events.shipment.ShipmentCompleted;
import com.yarnshipment.events.shipment.ShipmentCreated;
import com.yarnshipment.events.shipment.ShipmentDispatched;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sevkiyat olaylarını (oluşturma, çıkış, tamamlanma, iptal) zaman ve fotoğraf kuralları için izler; taşıyıcıdan
 * bağımsızdır. Diğer olaylar kullanılmaz ve yok sayılır.
 *
 * <p>Teslim: {@link Delivery#BEFORE_COMMIT}. Sevkiyat kaydı bu modülün kural işinin girdisidir; tüketici hatasında
 * sevkiyat işlemi geri alınır, böylece iki modül birbirinden ayrışmaz.
 */
@Component
final class ShipmentEventsConsumer implements DomainEventConsumer {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final RegisterShipmentUseCase registerShipmentUseCase;
    private final TrackShipmentUseCase trackShipmentUseCase;

    ShipmentEventsConsumer(RegisterShipmentUseCase registerShipmentUseCase, TrackShipmentUseCase trackShipmentUseCase) {
        this.registerShipmentUseCase = registerShipmentUseCase;
        this.trackShipmentUseCase = trackShipmentUseCase;
    }

    @Override
    public Delivery delivery() {
        return Delivery.BEFORE_COMMIT;
    }

    @Override
    public void consume(IncomingEvent event) {
        switch (event.type()) {
            case ShipmentCreated.TYPE -> onCreated(event);
            case ShipmentDispatched.TYPE -> {
                ShipmentDispatched dispatched = EVENT_READER.readValue(event.data(), ShipmentDispatched.class);
                trackShipmentUseCase.trackShipment(new TrackShipmentCommand(
                        dispatched.shipmentId(), Milestone.DISPATCHED, dispatched.dispatchedAt(), dispatched.netKg()));
            }
            case ShipmentCompleted.TYPE -> {
                ShipmentCompleted completed = EVENT_READER.readValue(event.data(), ShipmentCompleted.class);
                track(completed.shipmentId(), Milestone.COMPLETED, completed.completedAt());
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
