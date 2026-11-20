package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateCommand;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentSchedulePort;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentDays;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Müdür taslak sevkiyatın gününü değiştirir. Gün bugünden önce olamaz; taslak dışı sevkiyatın günü sabittir (depo
 * işlemleri başlamıştır). Aynı günü yeniden göndermek zararsızdır.
 */
@Service
public class UpdateShipmentDateService implements UpdateShipmentDateUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentSchedulePort schedulePort;
    private final Clock clock;

    public UpdateShipmentDateService(ScopedShipmentLoader shipments, ShipmentSchedulePort schedulePort, Clock clock) {
        this.shipments = shipments;
        this.schedulePort = schedulePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void updateShipmentDate(UpdateShipmentDateCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.loadForUpdate(actor, command.shipmentId(), Permission.SHIPMENT_CREATE);
        if (shipment.status() != ShipmentStatus.DRAFT) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.DRAFT);
        }
        if (command.shipmentDate().isBefore(ShipmentDays.today(clock))) {
            throw new InvalidNewShipmentException("shipmentDate", "must not be in the past");
        }
        if (command.shipmentDate().equals(shipment.shipmentDate())) {
            return;
        }
        schedulePort.updateShipmentDate(shipment.id(), command.shipmentDate(), actor.userId(), clock.instant());
    }
}
