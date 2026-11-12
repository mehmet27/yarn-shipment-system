package com.yarnshipment.discrepancy.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.events.shipment.ShipmentEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** {@code shipment.events.v1} Kafka dinleyicisi; olayı taşıyıcıdan bağımsız {@link ShipmentEventsConsumer}'a verir. */
@Component
final class ShipmentEventsListener {

    private final ShipmentEventsConsumer consumer;

    ShipmentEventsListener(ShipmentEventsConsumer consumer) {
        this.consumer = consumer;
    }

    @KafkaListener(topics = ShipmentEvents.TOPIC)
    void onShipmentEvent(ConsumerRecord<String, String> record) {
        consumer.consume(CloudEventHeaders.read(record));
    }
}
