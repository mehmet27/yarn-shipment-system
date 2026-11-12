package com.yarnshipment.shipment.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** {@code discrepancy.events.v1} Kafka dinleyicisi; olayı {@link DiscrepancyEventsConsumer}'a verir. */
@Component
final class DiscrepancyEventsListener {

    private final DiscrepancyEventsConsumer consumer;

    DiscrepancyEventsListener(DiscrepancyEventsConsumer consumer) {
        this.consumer = consumer;
    }

    @KafkaListener(topics = DiscrepancyEvents.TOPIC)
    void onDiscrepancyEvent(ConsumerRecord<String, String> record) {
        consumer.consume(CloudEventHeaders.read(record));
    }
}
