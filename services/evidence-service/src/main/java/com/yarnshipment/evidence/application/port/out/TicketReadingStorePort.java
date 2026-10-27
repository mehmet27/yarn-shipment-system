package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.ticket.TicketReading;
import java.util.Optional;
import java.util.UUID;

/** Fiş okumaları; her kanıt için en fazla bir kayıt. */
public interface TicketReadingStorePort {

    Optional<TicketReading> findReading(UUID evidenceId);

    void saveReading(TicketReading reading);
}
