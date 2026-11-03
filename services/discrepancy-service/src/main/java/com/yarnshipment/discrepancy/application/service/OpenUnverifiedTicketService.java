package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.ticket.OpenUnverifiedTicketCommand;
import com.yarnshipment.discrepancy.application.port.in.ticket.OpenUnverifiedTicketUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyOpeningPort;
import com.yarnshipment.discrepancy.application.port.out.OpenedDiscrepancy;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.domain.model.TicketChecks;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fiş "doğrulanamadı" (tasarım K0): kilo üç denemede okunamadı ve kullanıcı gerekçe seçti. Sevkiyat durmaz; Sarı
 * fotoğraf uyuşmazlığı açılır. Teslimden önce açılırsa karşılaştırmada sayılır ve sevkiyatı incelemeye alır. İzleme
 * kaydı olmayan sevkiyatta uyuşmazlık açılamaz; olay günlüğe yazılıp atlanır.
 */
@Service
public class OpenUnverifiedTicketService implements OpenUnverifiedTicketUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(OpenUnverifiedTicketService.class);

    private final ProcessedEventPort processedEvents;
    private final ShipmentWatchPort watchPort;
    private final DiscrepancyOpeningPort openingPort;
    private final Clock clock;

    public OpenUnverifiedTicketService(
            ProcessedEventPort processedEvents,
            ShipmentWatchPort watchPort,
            DiscrepancyOpeningPort openingPort,
            Clock clock) {
        this.processedEvents = processedEvents;
        this.watchPort = watchPort;
        this.openingPort = openingPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean openUnverifiedTicket(OpenUnverifiedTicketCommand command) {
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())) {
            return false;
        }
        Optional<WatchedShipment> shipment = watchPort.findShipment(command.shipmentId());
        if (shipment.isEmpty()) {
            LOG.warn(
                    "Unverified ticket on unwatched shipment skipped shipmentId={} evidenceId={}",
                    command.shipmentId(),
                    command.evidenceId());
            return false;
        }
        Instant now = clock.instant();
        openingPort.openDiscrepancy(new OpenedDiscrepancy(
                shipment.get(),
                TicketChecks.unverified(command.side(), command.reason()),
                now,
                SystemDefaults.dueAt(now),
                SystemDefaults.SYSTEM_ACTOR));
        return true;
    }
}
