package com.yarnshipment.reporting.adapter.in.messaging;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/** Kafka'dan gelmiş CloudEvents kaydı kurar. */
final class EventRecords {

    static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");
    static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a3");
    static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a4");
    static final String AT = "2026-11-03T08:00:00Z";

    private EventRecords() {}

    static ConsumerRecord<String, String> record(String topic, String type, String payload) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0L, SHIPMENT.toString(), payload);
        add(record, CloudEventHeaders.ID, UUID.randomUUID().toString());
        add(record, CloudEventHeaders.TYPE, type);
        add(record, CloudEventHeaders.SUBJECT, SHIPMENT.toString());
        add(record, CloudEventHeaders.TIME, Instant.parse(AT).toString());
        return record;
    }

    private static void add(ConsumerRecord<String, String> record, String key, String value) {
        record.headers().add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
