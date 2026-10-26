package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckUseCase;
import com.yarnshipment.shipment.application.port.out.CheckConclusionPort;
import com.yarnshipment.shipment.application.port.out.ProcessedEventPort;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
import com.yarnshipment.shipment.domain.check.CheckConclusion;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Karşılaştırma sonucunu sevkiyata uygular. Tekrar gelen olay ya da artık kontrolde olmayan sevkiyat (ör. aynı
 * sevkiyatın ikinci sonucu) durumu değiştirmez.
 */
@Service
public class ConcludeCheckService implements ConcludeCheckUseCase {

    private final ShipmentQueryPort shipments;
    private final ProcessedEventPort processedEvents;
    private final CheckConclusionPort conclusions;
    private final Clock clock;

    public ConcludeCheckService(
            ShipmentQueryPort shipments,
            ProcessedEventPort processedEvents,
            CheckConclusionPort conclusions,
            Clock clock) {
        this.shipments = shipments;
        this.processedEvents = processedEvents;
        this.conclusions = conclusions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void concludeCheck(ConcludeCheckCommand command) {
        Shipment shipment = shipments
                .findShipment(command.shipmentId())
                .orElseThrow(() -> new ShipmentNotFoundException(command.shipmentId()));
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())
                || shipment.status() != ShipmentStatus.CHECKING) {
            return;
        }
        conclusions.recordConclusion(CheckConclusion.of(shipment, command.outcome(), clock.instant()));
    }
}
