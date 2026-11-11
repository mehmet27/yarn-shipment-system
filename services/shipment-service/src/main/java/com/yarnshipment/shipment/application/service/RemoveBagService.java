package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentBagRemovalPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Çuvalı taslak sevkiyattan sıra numarasıyla çıkarır; numara listede yoksa sessizce geçer. */
@Service
public class RemoveBagService implements RemoveBagUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentBagRemovalPort removalPort;
    private final Clock clock;

    public RemoveBagService(ScopedShipmentLoader shipments, ShipmentBagRemovalPort removalPort, Clock clock) {
        this.shipments = shipments;
        this.removalPort = removalPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void removeBag(RemoveBagCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DISPATCH_RECORD);
        shipment.requireBagsAssignable();
        removalPort.remove(shipment.id(), command.seqNo(), actor.userId(), clock.instant());
    }
}
