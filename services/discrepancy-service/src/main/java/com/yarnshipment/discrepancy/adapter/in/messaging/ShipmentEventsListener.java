package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryCommand;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryUseCase;
import com.yarnshipment.events.shipment.DeliveryReceived;
import com.yarnshipment.events.shipment.ShipmentEvents;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code shipment.events.v1} dinleyicisi. Yalnızca teslim girişini karşılaştırmaya çevirir; diğer sevkiyat olayları
 * bu adımda kullanılmaz ve onaylanır.
 */
@Component
final class ShipmentEventsListener {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final CompareDeliveryUseCase compareDeliveryUseCase;

    ShipmentEventsListener(CompareDeliveryUseCase compareDeliveryUseCase) {
        this.compareDeliveryUseCase = compareDeliveryUseCase;
    }

    @KafkaListener(topics = ShipmentEvents.TOPIC)
    void onShipmentEvent(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        if (!DeliveryReceived.TYPE.equals(event.type())) {
            return;
        }
        DeliveryReceived delivery = EVENT_READER.readValue(event.data(), DeliveryReceived.class);
        compareDeliveryUseCase.compareDelivery(new CompareDeliveryCommand(
                event.id(),
                event.type(),
                actorOf(event),
                DeliveryReceivedMapper.toInput(delivery),
                DeliveryReceivedMapper.toEntries(delivery)));
    }

    /** Teslim girişini fasoncu yapar; olay her zaman onun kimliğini taşır (ShipmentTransitionWriter). */
    private static UUID actorOf(IncomingEvent event) {
        String actorId = event.actorId();
        if (actorId == null) {
            throw new IllegalArgumentException("DeliveryReceived without actorId eventId=" + event.id());
        }
        return UUID.fromString(actorId);
    }
}
