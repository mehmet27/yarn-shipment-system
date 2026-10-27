package com.yarnshipment.discrepancy.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.application.port.in.RecordTicketReadingCommand;
import com.yarnshipment.discrepancy.application.port.out.TicketReadingPort;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordTicketReadingServiceTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d002");

    private final TicketReadingPort ticketReadings = mock(TicketReadingPort.class);
    private final RecordTicketReadingService service = new RecordTicketReadingService(ticketReadings);

    @Test
    @DisplayName("Okuma yükleyen kişiyle saklanır")
    void shouldSaveReading() {
        TicketReading reading = TestTickets.reading(TicketSide.DISPATCH_SCALE, TestTickets.weights());

        service.recordTicketReading(new RecordTicketReadingCommand(ACTOR, reading));

        verify(ticketReadings).saveReading(reading, ACTOR);
    }
}
