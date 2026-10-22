package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.AssignBagsCommand;
import com.yarnshipment.shipment.application.port.in.AssignBagsUseCase;
import com.yarnshipment.shipment.application.port.out.BagAssignmentPort;
import com.yarnshipment.shipment.application.port.out.BagReservationPort;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çuvalları taslak sevkiyata atar. Önce çuval servisinde ayrılır (tek kullanım garantisi orada), sonra sevkiyatın
 * kendi listesine yazılır. Yerel yazma başarısız olursa ayırma kalır; aynı istek tekrarlandığında ayırma değişmeden
 * geçer ve yazma tamamlanır.
 */
@Service
public class AssignBagsService implements AssignBagsUseCase {

    private final ScopedShipmentLoader shipments;
    private final BagReservationPort reservationPort;
    private final BagAssignmentPort assignmentPort;
    private final Clock clock;

    public AssignBagsService(
            ScopedShipmentLoader shipments,
            BagReservationPort reservationPort,
            BagAssignmentPort assignmentPort,
            Clock clock) {
        this.shipments = shipments;
        this.reservationPort = reservationPort;
        this.assignmentPort = assignmentPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int assignBags(AssignBagsCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DISPATCH_RECORD);
        shipment.requireBagsAssignable();
        List<ReservedBag> reserved = reservationPort.reserve(shipment.id(), command.uniqueKeys());
        return assignmentPort.assign(
                new BagAssignment(shipment.id(), reserved, command.scanMethod()), actor.userId(), clock.instant());
    }
}
