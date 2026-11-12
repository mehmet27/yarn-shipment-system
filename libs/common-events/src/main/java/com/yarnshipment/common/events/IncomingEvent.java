package com.yarnshipment.common.events;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Tüketiciye teslim edilen bir olay.
 *
 * @param id CloudEvents {@code id}; idempotency anahtarı (EVT-05)
 * @param type CloudEvents {@code type}
 * @param subject bölümleme kimliği (çoğunlukla shipmentId)
 * @param time olayın gerçekleştiği zaman
 * @param actorId olayı doğuran kullanıcı; sistem olaylarında boş
 * @param data olay gövdesi (JSON)
 * @param topic olayın yayınlandığı konu; denetim kaydında varlık türünü belirler
 * @param source CloudEvents {@code source}, ör. {@code /yarn-shipment/shipment-service}
 * @param headers ek başlıklar (actorRoles, correlationId); taşıyıcıdan bağımsız
 */
public record IncomingEvent(
        UUID id,
        String type,
        UUID subject,
        Instant time,
        @Nullable String actorId,
        String data,
        @Nullable String topic,
        @Nullable String source,
        Map<String, String> headers) {

    /** Zarf bilgisi bilinmeyen (yalnızca gövdesi okunan) olay. */
    public IncomingEvent(UUID id, String type, UUID subject, Instant time, @Nullable String actorId, String data) {
        this(id, type, subject, time, actorId, data, null, null, Map.of());
    }

    public IncomingEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(data, "data");
        headers = Map.copyOf(headers);
    }
}
