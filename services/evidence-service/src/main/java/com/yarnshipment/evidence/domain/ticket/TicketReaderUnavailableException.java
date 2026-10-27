package com.yarnshipment.evidence.domain.ticket;

/** Okuma kapalı ya da modele ulaşılamadı; istemci okumasız devam eder. */
public final class TicketReaderUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TicketReaderUnavailableException(String message) {
        super(message);
    }

    public TicketReaderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
