package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceCommand;
import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyOpeningPort;
import com.yarnshipment.discrepancy.application.port.out.OpenedDiscrepancy;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.domain.rule.EvidenceChecks;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aynı dosya başka bir sevkiyatta daha önce yüklendiyse yeni kanıtın sevkiyatında {@code PHOTO_REUSED} açar (eksik
 * listesi 6.3). Sevkiyat teslimden önceyse uyuşmazlık karşılaştırmada sayılır ve sevkiyatı incelemeye alır; sevkiyat
 * tamamlandıysa durumu değişmez, uyuşmazlık ayrıca incelenir. Lokasyon ve fasoncu izleme kaydından gelir; bu servis
 * izlemeye başlamadan önce açılmış sevkiyatta uyuşmazlık açılamaz ve olay günlüğe yazılıp atlanır.
 */
@Service
public class OpenReusedEvidenceService implements OpenReusedEvidenceUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(OpenReusedEvidenceService.class);

    private final ProcessedEventPort processedEvents;
    private final ShipmentWatchPort watchPort;
    private final DiscrepancyOpeningPort openingPort;
    private final Clock clock;

    public OpenReusedEvidenceService(
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
    public boolean openReusedEvidence(OpenReusedEvidenceCommand command) {
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())) {
            return false;
        }
        Optional<WatchedShipment> shipment = watchPort.findShipment(command.shipmentId());
        if (shipment.isEmpty()) {
            LOG.warn(
                    "Evidence reuse on unwatched shipment skipped shipmentId={} evidenceId={}",
                    command.shipmentId(),
                    command.evidenceId());
            return false;
        }
        Instant now = clock.instant();
        openingPort.openDiscrepancy(new OpenedDiscrepancy(
                shipment.get(),
                EvidenceChecks.reused(command.evidenceId(), command.duplicateOfId()),
                now,
                SystemDefaults.dueAt(now),
                SystemDefaults.SYSTEM_ACTOR));
        return true;
    }
}
