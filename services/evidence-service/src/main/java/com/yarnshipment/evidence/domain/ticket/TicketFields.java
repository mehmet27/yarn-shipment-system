package com.yarnshipment.evidence.domain.ticket;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Fişin kilo dışındaki alanları; okunamayan alan boştur ve yeniden çekim gerektirmez.
 *
 * @param plate plaka
 * @param weighedAt fişteki tartım zamanı
 * @param ticketNo fiş numarası
 * @param scaleName kantar veya firma adı
 */
public record TicketFields(
        @Nullable String plate,
        @Nullable Instant weighedAt,
        @Nullable String ticketNo,
        @Nullable String scaleName) {

    public static final TicketFields EMPTY = new TicketFields(null, null, null, null);
}
