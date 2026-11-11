package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.AssignBagsUseCase;
import com.yarnshipment.shipment.application.port.out.BagAssignmentPort;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çuvalları taslak sevkiyata ekler: depo her çuvalı kiloyla yazar, sıra numarası sevkiyattaki son numaradan devam eder.
 * Çuval anahtarı ya da başka servis kullanılmaz.
 */
@Service
public class AssignBagsService implements AssignBagsUseCase {

    private final ScopedShipmentLoader shipments;
    private final BagAssignmentPort assignmentPort;
    private final Clock clock;

    public AssignBagsService(ScopedShipmentLoader shipments, BagAssignmentPort assignmentPort, Clock clock) {
        this.shipments = shipments;
        this.assignmentPort = assignmentPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int assignBags(AssignBagsCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DISPATCH_RECORD);
        shipment.requireBagsAssignable();
        return assignmentPort.assign(
                new BagAssignment(shipment.id(), command.weightsKg()), actor.userId(), clock.instant());
    }
}
