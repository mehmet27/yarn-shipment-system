package com.yarnshipment.common.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code outbox_event} tablosunun gönderilmemiş bir satırı.
 *
 * @param id CloudEvents {@code id}
 * @param createdAt bölümleme anahtarı; satır {@code (id, createdAt)} ile bulunur
 * @param aggregateId CloudEvents {@code subject} ve Kafka mesaj anahtarı
 * @param eventType CloudEvents {@code type}
 * @param payload olay gövdesi (JSON)
 * @param headers ek başlıklar: actorId, actorRoles, correlationId
 * @param occurredAt CloudEvents {@code time}
 */
public record OutboxEvent(
        UUID id,
        Instant createdAt,
        UUID aggregateId,
        String eventType,
        String payload,
        Map<String, String> headers,
        Instant occurredAt) {

    public OutboxEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(payload, "payload");
        headers = Map.copyOf(headers);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
