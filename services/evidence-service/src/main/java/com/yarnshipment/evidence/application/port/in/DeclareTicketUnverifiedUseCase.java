package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.ticket.TicketUnverified;

/**
 * Kilosu üç denemede okunamayan fiş için gerekçe yazar ve "fiş doğrulanamadı" olayını yayımlar. Yalnızca fişi yükleyen
 * ister; tekrar çağrılırsa ilk kayıt döner.
 */
public interface DeclareTicketUnverifiedUseCase {

    TicketUnverified declareUnverified(DeclareTicketUnverifiedCommand command);
}
