package com.yarnshipment.audit.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.evidence.EvidenceEvents;
import com.yarnshipment.events.shipment.ShipmentEvents;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Tüm servislerin olay konularını dinler ve her olayı {@link AuditEventsConsumer}'a verir. Zarfı bozuk olay kalıcı
 * hatadır ve DLT'ye gider.
 */
@Component
final class AuditEventsListener {

    private final AuditEventsConsumer consumer;

    AuditEventsListener(AuditEventsConsumer consumer) {
        this.consumer = consumer;
    }

    @KafkaListener(topics = {ShipmentEvents.TOPIC, EvidenceEvents.TOPIC, DiscrepancyEvents.TOPIC})
    void onEvent(ConsumerRecord<String, String> record) {
        consumer.consume(CloudEventHeaders.read(record));
    }
}
