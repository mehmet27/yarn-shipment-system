package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CancelShipmentUseCase;
import com.yarnshipment.shipment.application.port.out.BagReservationPort;
import com.yarnshipment.shipment.application.port.out.ShipmentCancellationPort;
import com.yarnshipment.shipment.domain.cancellation.Cancellation;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yola çıkmamış sevkiyatı iptal eder. Önce yerel iptal yazılır, sonra çuval servisinde rezervasyonlar bırakılır.
 * Bırakma başarısız olursa istisna transaction'ı geri alır; iptal de yazılmamış olur ve istek güvenle tekrarlanır
 * (bırakma tekrarında zararsızdır). Zaten iptal edilmiş sevkiyata gelen ikinci istek başarılı sayılır.
 */
@Service
public class CancelShipmentService implements CancelShipmentUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentCancellationPort cancellationPort;
    private final BagReservationPort reservationPort;
    private final Clock clock;

    public CancelShipmentService(
            ScopedShipmentLoader shipments,
            ShipmentCancellationPort cancellationPort,
            BagReservationPort reservationPort,
            Clock clock) {
        this.shipments = shipments;
        this.cancellationPort = cancellationPort;
        this.reservationPort = reservationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void cancelShipment(CancelShipmentCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.SHIPMENT_CANCEL);
        if (shipment.status() == ShipmentStatus.CANCELLED) {
            return;
        }
        Cancellation cancellation = Cancellation.of(shipment, command.reason(), clock.instant());
        cancellationPort.cancel(cancellation, actor.userId());
        reservationPort.release(shipment.id(), cancellation.reason());
    }
}
