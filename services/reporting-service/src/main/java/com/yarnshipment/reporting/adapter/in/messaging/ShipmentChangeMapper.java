package com.yarnshipment.reporting.adapter.in.messaging;

import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.shipment.DeliveryReceived;
import com.yarnshipment.events.shipment.ShipmentArrived;
import com.yarnshipment.events.shipment.ShipmentCancelled;
import com.yarnshipment.events.shipment.ShipmentCompleted;
import com.yarnshipment.events.shipment.ShipmentCreated;
import com.yarnshipment.events.shipment.ShipmentDispatched;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Sevkiyat olaylarını okuma modeli değişikliğine çevirir. */
final class ShipmentChangeMapper {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private ShipmentChangeMapper() {}

    /** Bu servisin kullanmadığı olay için {@code null}. */
    static @Nullable ReadModelChange toChange(IncomingEvent event) {
        return switch (event.type()) {
            case ShipmentCreated.TYPE -> created(event);
            case ShipmentDispatched.TYPE -> dispatched(event);
            case ShipmentArrived.TYPE -> arrived(event);
            case ShipmentCompleted.TYPE -> completed(event);
            case ShipmentCancelled.TYPE ->
                new ReadModelChange.ShipmentCancelled(EVENT_READER
                        .readValue(event.data(), ShipmentCancelled.class)
                        .shipmentId());
            case DeliveryReceived.TYPE -> delivered(event);
            default -> null;
        };
    }

    private static ReadModelChange created(IncomingEvent event) {
        ShipmentCreated created = EVENT_READER.readValue(event.data(), ShipmentCreated.class);
        return new ReadModelChange.ShipmentCreated(
                created.shipmentId(),
                created.shipmentNo(),
                created.siteId(),
                created.subcontractorId(),
                created.createdAt());
    }

    private static ReadModelChange dispatched(IncomingEvent event) {
        ShipmentDispatched dispatched = EVENT_READER.readValue(event.data(), ShipmentDispatched.class);
        return new ReadModelChange.ShipmentDispatched(
                dispatched.shipmentId(), dispatched.driverId(), dispatched.dispatchedAt());
    }

    private static ReadModelChange arrived(IncomingEvent event) {
        ShipmentArrived arrived = EVENT_READER.readValue(event.data(), ShipmentArrived.class);
        return new ReadModelChange.ShipmentArrived(arrived.shipmentId(), arrived.driverId());
    }

    private static ReadModelChange completed(IncomingEvent event) {
        ShipmentCompleted completed = EVENT_READER.readValue(event.data(), ShipmentCompleted.class);
        return new ReadModelChange.ShipmentCompleted(completed.shipmentId(), completed.completedAt());
    }

    private static ReadModelChange delivered(IncomingEvent event) {
        DeliveryReceived delivery = EVENT_READER.readValue(event.data(), DeliveryReceived.class);
        DeliveryReceived.Weighing dispatch = delivery.dispatch();
        return new ReadModelChange.DeliveryReceived(
                delivery.shipmentId(),
                dispatch == null ? null : dispatch.netKg(),
                delivery.delivery().netKg());
    }
}
