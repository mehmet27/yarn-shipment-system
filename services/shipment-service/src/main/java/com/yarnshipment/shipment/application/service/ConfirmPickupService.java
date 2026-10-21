package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupCommand;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort;
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

    private static final String RESOURCE_TYPE = "Shipment";

    private final ShipmentQueryPort shipmentQueryPort;
    private final ShipmentCommandPort shipmentCommandPort;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public ConfirmPickupService(
            ShipmentQueryPort shipmentQueryPort,
            ShipmentCommandPort shipmentCommandPort,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.shipmentQueryPort = shipmentQueryPort;
        this.shipmentCommandPort = shipmentCommandPort;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void confirmPickup(ConfirmPickupCommand command) {
        Shipment shipment = shipmentQueryPort
                .findShipment(command.shipmentId())
                .orElseThrow(() -> new ShipmentNotFoundException(command.shipmentId()));
        Actor actor = command.actor();
        scopeGuard.validateInScope(
                actor,
                new ScopedResource(
                        RESOURCE_TYPE,
                        shipment.id(),
                        shipment.siteId(),
                        shipment.subcontractorId(),
                        shipment.driverId()),
                Permission.DRIVER_DECLARATION_RECORD.name());
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
