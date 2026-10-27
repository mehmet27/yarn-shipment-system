package com.yarnshipment.evidence.domain.ticket;

/** Okumanın sonucu: brüt ve dara güvenle okunduysa {@link #READ}, yoksa yeniden çekim gerekir. */
public enum TicketReadingStatus {
    READ,
    UNREADABLE
}
