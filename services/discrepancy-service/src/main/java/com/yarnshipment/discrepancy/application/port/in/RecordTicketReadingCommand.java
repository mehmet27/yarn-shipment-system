package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import java.util.Objects;
import java.util.UUID;

/**
 * Kantar fişi okumasının kaydı.
 *
 * @param actorId fotoğrafı yükleyen kullanıcı; kaydın {@code created_by} değeri
 * @param reading okuma
 */
public record RecordTicketReadingCommand(UUID actorId, TicketReading reading) {

    public RecordTicketReadingCommand {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(reading, "reading");
    }
}
