package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewUseCase;
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
 * Tüm uyuşmazlıkları çözülen sevkiyatı tamamlar. Tekrar gelen olay ya da artık incelemede olmayan sevkiyat (ör. iptal
 * edilmiş) durumu değiştirmez.
 */
@Service
public class ConcludeReviewService implements ConcludeReviewUseCase {

    private final ShipmentQueryPort shipments;
    private final ProcessedEventPort processedEvents;
    private final CheckConclusionPort conclusions;
    private final Clock clock;

    public ConcludeReviewService(
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
    public void concludeReview(ConcludeReviewCommand command) {
        Shipment shipment = shipments
                .findShipment(command.shipmentId())
                .orElseThrow(() -> new ShipmentNotFoundException(command.shipmentId()));
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())
                || shipment.status() != ShipmentStatus.IN_REVIEW) {
            return;
        }
        conclusions.recordConclusion(CheckConclusion.afterReview(shipment, clock.instant()));
    }
}
