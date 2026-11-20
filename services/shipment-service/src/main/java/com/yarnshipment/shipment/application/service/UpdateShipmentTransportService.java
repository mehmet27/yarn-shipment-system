package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.ShipmentAction;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportCommand;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentTransportPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Müdür tamamlanmamış sevkiyatın şoför ve araç bilgisini düzeltir; yolda araç değişebilir. Tamamlanmış ya da iptal
 * edilmiş sevkiyat değişmez.
 */
@Service
public class UpdateShipmentTransportService implements UpdateShipmentTransportUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentTransportPort transportPort;
    private final Clock clock;

    public UpdateShipmentTransportService(
            ScopedShipmentLoader shipments, ShipmentTransportPort transportPort, Clock clock) {
        this.shipments = shipments;
        this.transportPort = transportPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void updateShipmentTransport(UpdateShipmentTransportCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.loadForUpdate(actor, command.shipmentId(), Permission.SHIPMENT_CREATE);
        if (!ShipmentAction.UPDATE_TRANSPORT.isAllowedIn(shipment.status())) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.DRAFT);
        }
        transportPort.updateTransport(shipment.id(), command.transport(), actor.userId(), clock.instant());
    }
}
