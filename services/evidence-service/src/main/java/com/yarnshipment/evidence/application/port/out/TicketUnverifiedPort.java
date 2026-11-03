package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.ticket.TicketUnverified;
import java.util.Optional;
import java.util.UUID;

/** Fiş "doğrulanamadı" gerekçeleri; her okuma için en fazla bir kayıt. */
public interface TicketUnverifiedPort {

    Optional<TicketUnverified> findUnverified(UUID evidenceId);

    /** Gerekçeyi ve olayını yazar; aynı kanıt için kayıt zaten varsa ilki kalır ve olay tekrar yazılmaz. */
    void saveUnverified(TicketUnverified unverified);
}
