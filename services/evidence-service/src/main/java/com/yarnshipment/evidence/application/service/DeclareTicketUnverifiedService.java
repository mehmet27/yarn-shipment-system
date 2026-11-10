package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedCommand;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedUseCase;
import com.yarnshipment.evidence.application.port.out.TicketReadingStorePort;
import com.yarnshipment.evidence.application.port.out.TicketUnverifiedPort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.ticket.TicketNotUnreadableException;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketUnverified;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fiş "doğrulanamadı" (tasarım K0). Okumayı isteyen kişi, son denemenin okunamayan fişi için gerekçe seçer; kontrol
 * uyuşmazlık kuyruğuna düşer, tartım adımı ilerler.
 */
@Service
public class DeclareTicketUnverifiedService implements DeclareTicketUnverifiedUseCase {

    private static final String ACTION = "EVIDENCE_TICKET_UNVERIFIED";

    private final UploadIntentPort intents;
    private final TicketReadingStorePort readings;
    private final TicketUnverifiedPort unverifiedPort;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public DeclareTicketUnverifiedService(
            UploadIntentPort intents,
            TicketReadingStorePort readings,
            TicketUnverifiedPort unverifiedPort,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.intents = intents;
        this.readings = readings;
        this.unverifiedPort = unverifiedPort;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TicketUnverified declareUnverified(DeclareTicketUnverifiedCommand command) {
        UploadIntent intent = intents.findIntent(command.evidenceId())
                .filter(found -> found.requestedBy().equals(command.actor().userId()))
                .orElseThrow(() -> new UploadIntentNotFoundException(command.evidenceId()));
        scopeGuard.validateInScope(
                command.actor(), ShipmentScopes.of(intent.shipmentId(), intent.siteId()), ACTION);
        Optional<TicketUnverified> existing = unverifiedPort.findUnverified(intent.id());
        if (existing.isPresent()) {
            return existing.get();
        }
        TicketReading reading =
                readings.findReading(intent.id()).orElseThrow(() -> new TicketNotUnreadableException(intent.id()));
        TicketUnverified unverified = TicketUnverified.declare(reading, command.reason(), clock.instant());
        unverifiedPort.saveUnverified(unverified);
        return unverified;
    }
}
