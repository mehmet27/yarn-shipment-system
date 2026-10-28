package com.yarnshipment.discrepancy.domain.ticket;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir kantar fişi fotoğrafının okuması (evidence-service). Kilolar yalnızca güvenle okunduysa doludur.
 *
 * @param evidenceId fotoğraf
 * @param shipmentId sevkiyat
 * @param side fişin tarafı
 * @param weights okunan kilolar; fiş okunamadıysa boş
 * @param fields fişin diğer alanları
 * @param problems okuma sorunları
 * @param readAt okuma zamanı
 */
public record TicketReading(
        UUID evidenceId,
        UUID shipmentId,
        TicketSide side,
        @Nullable ScaleWeights weights,
        TicketFields fields,
        List<String> problems,
        Instant readAt) {

    public TicketReading {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(fields, "fields");
        problems = List.copyOf(problems);
        Objects.requireNonNull(readAt, "readAt");
    }

    public boolean read() {
        return weights != null;
    }
}
