package com.yarnshipment.discrepancy.application.port.in.ticket;

import com.yarnshipment.common.security.Actor;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Fiş okumasını "AI yanıldı" olarak işaretleme komutu. */
public record MarkTicketMisreadCommand(
        Actor actor,
        UUID discrepancyId,
        UUID evidenceId,
        @Nullable String note) {}
