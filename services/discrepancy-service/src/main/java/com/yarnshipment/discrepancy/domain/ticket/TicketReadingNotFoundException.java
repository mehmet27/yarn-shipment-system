package com.yarnshipment.discrepancy.domain.ticket;

import java.util.UUID;

/** Bu uyuşmazlığın sevkiyatında böyle bir fiş okuması yok. */
public final class TicketReadingNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TicketReadingNotFoundException(UUID evidenceId) {
        super("Ticket reading not found evidenceId=" + evidenceId);
    }
}
