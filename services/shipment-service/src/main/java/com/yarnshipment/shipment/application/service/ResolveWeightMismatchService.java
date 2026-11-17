package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CancelShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchCommand;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchUseCase;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.InvalidWeightDecisionException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kantar uyuşmazlığındaki sevkiyata müdür karar verir (süreç analizi 5.4): iptal (çuvallar serbest kalır), çuval
 * ekle/çıkar (taslağa döner). Gerekçe zorunludur. Reçete güncelleme tartı kontrolü istediği için ayrı use case'tedir.
 */
@Service
public class ResolveWeightMismatchService implements ResolveWeightMismatchUseCase {

    private static final int MAX_REASON_LENGTH = 500;

    private final ScopedShipmentLoader shipments;
    private final WeightDecisionPort decisionPort;
    private final CancelShipmentUseCase cancelShipment;
    private final Clock clock;

    public ResolveWeightMismatchService(
            ScopedShipmentLoader shipments,
            WeightDecisionPort decisionPort,
            CancelShipmentUseCase cancelShipment,
            Clock clock) {
        this.shipments = shipments;
        this.decisionPort = decisionPort;
        this.cancelShipment = cancelShipment;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void resolve(ResolveWeightMismatchCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.SHIPMENT_CANCEL);
        String reason = command.reason().strip();
        if (reason.isEmpty() || reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidWeightDecisionException("reason", "required");
        }
        if (shipment.status() != ShipmentStatus.WEIGHT_MISMATCH) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.DRAFT);
        }
        if (command.decision() == WeightDecision.UPDATE_RECIPE) {
            throw new InvalidWeightDecisionException("decision", "use recipe update");
        }
        Instant now = clock.instant();
        UUID actorId = actor.userId();
        WeightDecisionPort.Audit audit = new WeightDecisionPort.Audit(actorId, now);
        decisionPort.recordDecision(shipment.id(), command.decision(), reason, audit);
        switch (command.decision()) {
            case CANCEL ->
                cancelShipment.cancelShipment(
                        new CancelShipmentCommand(actor, shipment.id(), reason, UUID.randomUUID()));
            case ADD_REMOVE_BAGS -> decisionPort.backToDraft(shipment.id(), reason, audit);
            case UPDATE_RECIPE -> throw new IllegalStateException("handled above");
        }
    }
}
