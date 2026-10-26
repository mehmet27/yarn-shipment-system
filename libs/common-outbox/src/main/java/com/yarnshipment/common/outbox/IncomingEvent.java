package com.yarnshipment.common.outbox;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kafka'dan okunan bir olay.
 *
 * @param id CloudEvents {@code id}; idempotency anahtarı (EVT-05)
 * @param type CloudEvents {@code type}
 * @param subject bölümleme kimliği (çoğunlukla shipmentId)
 * @param time olayın gerçekleştiği zaman
 * @param actorId olayı doğuran kullanıcı; sistem olaylarında boş
 * @param data olay gövdesi (JSON)
 */
public record IncomingEvent(
        UUID id,
        String type,
        UUID subject,
        Instant time,
        @Nullable String actorId,
        String data) {

    public IncomingEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(data, "data");
    }
}
