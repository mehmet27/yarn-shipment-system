package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupCommand;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Şoförün teslim alma beyanı. Aynı sevkiyat için ikinci istek yeni kayıt üretmez: beyan zaten varsa ilk sonuç
 * (204) tekrar döner (API-04). Anahtar saklayan ortak mekanizma {@code common-web} ile gelecek.
 */
@Service
public class ConfirmPickupService implements ConfirmPickupUseCase {

    private final ScopedShipmentLoader shipments;
    private final ShipmentCommandPort shipmentCommandPort;
    private final Clock clock;

    public ConfirmPickupService(ScopedShipmentLoader shipments, ShipmentCommandPort shipmentCommandPort, Clock clock) {
        this.shipments = shipments;
        this.shipmentCommandPort = shipmentCommandPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void confirmPickup(ConfirmPickupCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.DRIVER_DECLARATION_RECORD);
        if (isRepeatedRequest(shipment)) {
            return;
        }
        shipmentCommandPort.recordPickup(shipment.confirmPickup(driverIdOf(actor), clock.instant()), actor.userId());
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.LOADED && shipmentCommandPort.hasPickupConfirmation(shipment.id());
    }

    /**
     * İzin yalnızca şoför rolündedir ve şoför token'ı {@code driver_id} taşımadan oluşturulamaz (common-security);
     * boş olması yetki matrisinin bozulduğunu gösterir.
     */
    private static UUID driverIdOf(Actor actor) {
        return Objects.requireNonNull(actor.driverId(), "Pickup confirmation requires a driver actor");
    }
}
