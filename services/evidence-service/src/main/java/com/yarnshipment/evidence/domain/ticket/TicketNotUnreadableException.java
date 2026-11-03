package com.yarnshipment.evidence.domain.ticket;

import java.util.UUID;

/** "Doğrulanamadı" yalnızca okunmuş ve kilosu okunamamış fiş için seçilebilir. */
public final class TicketNotUnreadableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TicketNotUnreadableException(UUID evidenceId) {
        super("Ticket is not read as unreadable evidenceId=" + evidenceId);
    }
}
