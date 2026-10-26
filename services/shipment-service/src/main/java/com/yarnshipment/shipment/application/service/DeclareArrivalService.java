package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalCommand;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.domain.arrival.ArrivalDeclaration;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Şoförün varış beyanı. Aynı sevkiyat için ikinci istek yeni kayıt üretmez: beyan zaten varsa ilk sonuç (204) tekrar
 * döner (API-04).
 */
@Service
public class DeclareArrivalService implements DeclareArrivalUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentCommandPort shipmentCommandPort;
    private final Clock clock;

    public DeclareArrivalService(ScopedShipmentLoader shipments, ShipmentCommandPort shipmentCommandPort, Clock clock) {
        this.shipments = shipments;
        this.shipmentCommandPort = shipmentCommandPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void declareArrival(DeclareArrivalCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DRIVER_DECLARATION_RECORD);
        if (isRepeatedRequest(shipment)) {
            return;
        }
        shipmentCommandPort.recordArrival(
                ArrivalDeclaration.of(shipment, driverIdOf(actor), command.sealIntact(), clock.instant()),
                actor.userId());
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.IN_TRANSIT
                && shipmentCommandPort.hasArrivalDeclaration(shipment.id());
    }

    /** İzin yalnızca şoför rolündedir ve şoför token'ı {@code driver_id} taşır (bkz. {@link ConfirmPickupService}). */
    private static UUID driverIdOf(Actor actor) {
        return Objects.requireNonNull(actor.driverId(), "Arrival declaration requires a driver actor");
    }
}
