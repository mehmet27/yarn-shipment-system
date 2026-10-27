package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingUseCase;
import com.yarnshipment.discrepancy.application.port.out.TicketReadingPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kantar fişi okumasını saklar. Okuma teslim girişinden sonra gelirse karşılaştırma tekrar yapılmaz; o sevkiyatın fiş
 * kontrolleri eksik kalır, sevkiyat durmaz.
 */
@Service
public class RecordTicketReadingService implements RecordTicketReadingUseCase {

    private final TicketReadingPort ticketReadings;

    public RecordTicketReadingService(TicketReadingPort ticketReadings) {
        this.ticketReadings = ticketReadings;
    }

    @Override
    @Transactional
    public void recordTicketReading(RecordTicketReadingCommand command) {
        ticketReadings.saveReading(command.reading(), command.actorId());
    }
}
