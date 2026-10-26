package com.yarnshipment.common.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.jspecify.annotations.Nullable;

/**
 * CloudEvents Kafka bağlaması, ikili kip (EVT-02): zarf alanları {@code ce_} önekli başlıklarda, olay gövdesi
 * mesaj değerinde taşınır. Outbox başlıkları (actorId, correlationId) aynı adla eklenir.
 */
public final class CloudEventHeaders {

    public static final String ID = "ce_id";
    public static final String SOURCE = "ce_source";
    public static final String TYPE = "ce_type";
    public static final String SUBJECT = "ce_subject";
    public static final String TIME = "ce_time";
    public static final String SPEC_VERSION = "ce_specversion";
    public static final String CONTENT_TYPE = "content-type";

    private static final String SPEC_VERSION_VALUE = "1.0";
    private static final String JSON = "application/json";

    private CloudEventHeaders() {}

    /** Outbox satırından Kafka mesajı kurar; anahtar bölümleme kimliğidir (EVT-04). */
    public static ProducerRecord<String, String> toRecord(String topic, String source, OutboxEvent event) {
        List<Header> headers = new ArrayList<>();
        headers.add(header(SPEC_VERSION, SPEC_VERSION_VALUE));
        headers.add(header(ID, event.id().toString()));
        headers.add(header(SOURCE, source));
        headers.add(header(TYPE, event.eventType()));
        headers.add(header(SUBJECT, event.aggregateId().toString()));
        headers.add(header(TIME, event.occurredAt().toString()));
        headers.add(header(CONTENT_TYPE, JSON));
        for (Map.Entry<String, String> extra : event.headers().entrySet()) {
            headers.add(header(extra.getKey(), extra.getValue()));
        }
        return new ProducerRecord<>(topic, null, event.aggregateId().toString(), event.payload(), headers);
    }

    /** Tüketici tarafı: zarf alanlarını okur. Zorunlu başlık eksikse kalıcı hatadır, tekrar denenmez (EVT-09). */
    public static IncomingEvent read(ConsumerRecord<String, String> record) {
        Headers headers = record.headers();
        return new IncomingEvent(
                UUID.fromString(required(headers, ID)),
                required(headers, TYPE),
                UUID.fromString(required(headers, SUBJECT)),
                Instant.parse(required(headers, TIME)),
                optional(headers, "actorId"),
                record.value());
    }

    private static Header header(String key, String value) {
        return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
    }

    private static String required(Headers headers, String key) {
        String value = optional(headers, key);
        if (value == null) {
            throw new IllegalArgumentException("Missing CloudEvents header " + key);
        }
        return value;
    }

    private static @Nullable String optional(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
