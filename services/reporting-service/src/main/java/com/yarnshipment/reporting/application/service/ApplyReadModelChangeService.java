package com.yarnshipment.reporting.application.service;

import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeUseCase;
import com.yarnshipment.reporting.application.port.out.ProcessedEventPort;
import com.yarnshipment.reporting.application.port.out.ReadModelPort;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import com.yarnshipment.reporting.domain.model.ShipmentNotInReadModelException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Olayları okuma modeline uygular. Olay kimliği aynı transaction'da kaydedilir; ikinci gelen olay atlanır. Sevkiyatı
 * okuma modelinde olmayan olay hata verir ve dinleyici hata politikasıyla tekrar denenir.
 */
@Service
public class ApplyReadModelChangeService implements ApplyReadModelChangeUseCase {

    private final ProcessedEventPort processedEvents;
    private final ReadModelPort readModel;
    private final ComparisonRecorder comparisons;

    ApplyReadModelChangeService(
            ProcessedEventPort processedEvents, ReadModelPort readModel, ComparisonRecorder comparisons) {
        this.processedEvents = processedEvents;
        this.readModel = readModel;
        this.comparisons = comparisons;
    }

    @Override
    @Transactional
    public void applyChange(ApplyReadModelChangeCommand command) {
        if (!processedEvents.markProcessed(command.eventId(), command.eventType())) {
            return;
        }
        switch (command.change()) {
            case ReadModelChange.ShipmentCreated created -> readModel.insertShipment(created);
            case ReadModelChange.ShipmentDispatched dispatched ->
                require(dispatched.shipmentId(), readModel.saveDispatched(dispatched));
            case ReadModelChange.ShipmentArrived arrived ->
                require(arrived.shipmentId(), readModel.saveArrived(arrived));
            case ReadModelChange.ShipmentCompleted completed ->
                require(completed.shipmentId(), readModel.saveCompleted(completed));
            case ReadModelChange.ShipmentCancelled cancelled ->
                require(cancelled.shipmentId(), readModel.saveCancelled(cancelled));
            case ReadModelChange.DeliveryReceived delivery ->
                require(delivery.shipmentId(), readModel.saveDelivery(delivery));
            case ReadModelChange.ComparisonCompleted comparison -> comparisons.record(comparison);
            case ReadModelChange.DiscrepancyOpened opened -> readModel.insertDiscrepancy(opened);
            case ReadModelChange.DiscrepancyResolved resolved -> readModel.saveDiscrepancyResolved(resolved);
        }
    }

    private static void require(UUID shipmentId, boolean found) {
        if (!found) {
            throw new ShipmentNotInReadModelException(shipmentId);
        }
    }
}
