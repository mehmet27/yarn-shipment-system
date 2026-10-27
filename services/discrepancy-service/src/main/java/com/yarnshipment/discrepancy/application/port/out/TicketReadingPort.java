package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import java.util.UUID;

/** Kantar fişi okumaları. */
public interface TicketReadingPort {

    /** Okumayı yazar; aynı fotoğrafın okuması zaten varsa ilki kalır. */
    void saveReading(TicketReading reading, UUID actorId);

    /** Her taraf için en son güvenle okunan fiş ve bu fişlerin başka sevkiyatlarda görülüp görülmediği. */
    ShipmentTickets findTickets(UUID shipmentId);
}
