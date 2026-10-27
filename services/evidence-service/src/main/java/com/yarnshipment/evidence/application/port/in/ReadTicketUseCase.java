package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.ticket.TicketReading;

/**
 * Yüklenmiş kantar fişi fotoğrafını okur ve güven kurallarından geçirir. Okumayı yalnızca yükleyen ister; aynı kanıt
 * için tekrar istek kayıtlı okumayı döner.
 */
public interface ReadTicketUseCase {

    TicketReading readTicket(ReadTicketCommand command);
}
