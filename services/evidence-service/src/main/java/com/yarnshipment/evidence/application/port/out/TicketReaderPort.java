package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.ticket.ReaderVersion;
import com.yarnshipment.evidence.domain.ticket.TicketScan;

/** Fiş fotoğrafını alanlara çeviren görüntü modeli. Karar vermez; yalnızca okur. */
public interface TicketReaderPort {

    /** Okumayla birlikte saklanacak model ve istem sürümü. */
    ReaderVersion readerVersion();

    /**
     * Fotoğrafı okur.
     *
     * @param image dosyanın baytları
     * @param mimeType dosya türü
     * @throws com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException okuma kapalıysa ya da modele
     *     ulaşılamazsa
     */
    TicketScan scanTicket(byte[] image, String mimeType);
}
