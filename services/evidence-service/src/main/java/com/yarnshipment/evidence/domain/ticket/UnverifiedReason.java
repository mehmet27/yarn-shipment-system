package com.yarnshipment.evidence.domain.ticket;

/** Fiş üç denemede okunamadığında kullanıcının seçtiği gerekçe (tasarım K0). */
public enum UnverifiedReason {
    /** Kantarın yazıcısı bozuk ya da fiş soluk basılmış. */
    PRINTER_BROKEN,
    /** Fiş yırtık, ıslak ya da solmuş. */
    TICKET_DAMAGED,
    /** Kantar fiş vermedi. */
    NO_TICKET,
    OTHER
}
