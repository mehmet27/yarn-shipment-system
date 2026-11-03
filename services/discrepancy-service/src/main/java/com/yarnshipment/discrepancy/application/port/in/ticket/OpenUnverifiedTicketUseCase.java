package com.yarnshipment.discrepancy.application.port.in.ticket;

/** Doğrulanamayan kantar fişi için sevkiyatta Sarı fotoğraf uyuşmazlığı açar (tasarım K0). */
public interface OpenUnverifiedTicketUseCase {

    /** Açıldıysa {@code true}; olay tekrar geldiyse ya da sevkiyat izlenmiyorsa {@code false}. */
    boolean openUnverifiedTicket(OpenUnverifiedTicketCommand command);
}
