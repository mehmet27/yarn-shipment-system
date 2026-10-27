package com.yarnshipment.evidence.adapter.out.ticket;

import com.yarnshipment.evidence.application.port.out.TicketReaderPort;
import com.yarnshipment.evidence.domain.ticket.ReaderVersion;
import com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;

/** Okuma kapalıyken kullanılır; her istek "okuma yapılamadı" ile döner, akış bugünkü gibi sürer. */
final class DisabledTicketReader implements TicketReaderPort {

    private static final ReaderVersion NONE = new ReaderVersion("none", "none");

    @Override
    public ReaderVersion readerVersion() {
        return NONE;
    }

    @Override
    public TicketScan scanTicket(byte[] image, String mimeType) {
        throw new TicketReaderUnavailableException("Ticket reading is disabled");
    }
}
