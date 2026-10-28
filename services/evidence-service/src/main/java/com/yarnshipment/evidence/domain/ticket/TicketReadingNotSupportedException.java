package com.yarnshipment.evidence.domain.ticket;

import java.util.UUID;

/** Yalnızca kantar fişi kanıtları ({@code DISPATCH_SCALE}, {@code DELIVERY_SCALE}) okunabilir. */
public final class TicketReadingNotSupportedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TicketReadingNotSupportedException(UUID evidenceId) {
        super("Evidence is not a scale ticket evidenceId=" + evidenceId);
    }
}
