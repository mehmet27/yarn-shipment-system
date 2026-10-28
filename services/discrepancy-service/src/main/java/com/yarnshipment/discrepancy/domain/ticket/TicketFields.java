package com.yarnshipment.discrepancy.domain.ticket;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Kantar fişinin kilo dışındaki alanları; okunamayan alan boştur.
 *
 * @param plate araç plakası
 * @param weighedAt tartım zamanı
 * @param ticketNo fiş numarası
 * @param scaleName kantarın ya da firmanın adı
 */
public record TicketFields(
        @Nullable String plate,
        @Nullable Instant weighedAt,
        @Nullable String ticketNo,
        @Nullable String scaleName) {}
