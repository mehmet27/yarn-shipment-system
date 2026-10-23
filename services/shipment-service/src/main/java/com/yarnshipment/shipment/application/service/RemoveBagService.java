package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase;
import com.yarnshipment.shipment.application.port.in.RemoveBagUseCase.RemoveBagCommand;
import com.yarnshipment.shipment.application.port.out.BagReservationPort;
import com.yarnshipment.shipment.application.port.out.ShipmentBagRemovalPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Çuvalı taslak sevkiyattan çıkarır. Önce yerel liste güncellenir, sonra çuval servisinde rezervasyon bırakılır;
 * bırakma başarısız olursa istisna çıkarmayı da geri alır ve istek güvenle tekrarlanır.
 */
@Service
public class RemoveBagService implements RemoveBagUseCase {

    static final String RELEASE_REASON = "Sevkiyattan çıkarıldı";

    private final ScopedShipmentLoader shipments;
    private final ShipmentBagRemovalPort removalPort;
    private final BagReservationPort reservationPort;
    private final Clock clock;

    public RemoveBagService(
            ScopedShipmentLoader shipments,
            ShipmentBagRemovalPort removalPort,
            BagReservationPort reservationPort,
            Clock clock) {
        this.shipments = shipments;
        this.removalPort = removalPort;
        this.reservationPort = reservationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void removeBag(RemoveBagCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DISPATCH_RECORD);
        shipment.requireBagsAssignable();
        removalPort.remove(shipment.id(), command.uniqueKey(), actor.userId(), clock.instant());
        reservationPort.releaseBag(shipment.id(), command.uniqueKey(), RELEASE_REASON);
    }
}
