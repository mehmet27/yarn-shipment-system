package com.yarnshipment.evidence.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.application.port.in.ReadTicketCommand;
import com.yarnshipment.evidence.application.port.in.ReadTicketUseCase;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.TicketReaderPort;
import com.yarnshipment.evidence.application.port.out.TicketReadingStorePort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketReadingNotSupportedException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Fiş okuması. Model çağrısı birkaç saniye sürdüğü için transaction dışında yapılır; sonuç tek satırlık kayıt olarak
 * yazılır. Kullanıcı önizleme ekranında bekler ve kilo okunamazsa yeniden çeker.
 */
@Service
public final class ReadTicketService implements ReadTicketUseCase {

    private static final String ACTION = "EVIDENCE_TICKET_READ";

    private final UploadIntentPort intents;
    private final EvidenceStorePort evidence;
    private final ObjectStoragePort storage;
    private final TicketReaderPort reader;
    private final TicketReadingStorePort readings;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public ReadTicketService(
            UploadIntentPort intents,
            EvidenceStorePort evidence,
            ObjectStoragePort storage,
            TicketReaderPort reader,
            TicketReadingStorePort readings,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.intents = intents;
        this.evidence = evidence;
        this.storage = storage;
        this.reader = reader;
        this.readings = readings;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    public TicketReading readTicket(ReadTicketCommand command) {
        UploadIntent intent = intents.findIntent(command.evidenceId())
                .filter(found -> found.requestedBy().equals(command.actor().userId()))
                .orElseThrow(() -> new UploadIntentNotFoundException(command.evidenceId()));
        scopeGuard.validateInScope(
                command.actor(), ShipmentScopes.of(command.actor(), intent.shipmentId(), intent.siteId()), ACTION);
        if (!TicketReading.isReadable(intent.file().type())) {
            throw new TicketReadingNotSupportedException(intent.id());
        }
        Optional<TicketReading> existing = readings.findReading(intent.id());
        if (existing.isPresent()) {
            return existing.get();
        }
        EvidenceFile file =
                evidence.findEvidence(intent.id()).orElseThrow(() -> new UploadNotCompletedException(intent.id()));
        byte[] image =
                storage.download(file.storageKey()).orElseThrow(() -> new UploadNotCompletedException(intent.id()));
        TicketScan scan = reader.scanTicket(image, file.stored().mimeType());
        TicketReading reading = TicketReading.evaluate(file, scan, reader.readerVersion(), clock.instant());
        readings.saveReading(reading);
        return reading;
    }
}
