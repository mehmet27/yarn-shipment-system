package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CreateShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.application.port.out.ShipmentCreationPort;
import com.yarnshipment.shipment.domain.model.NewShipment;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sevkiyat taslağı oluşturur. Lokasyon, kullanıcının yetkili olduğu lokasyonlardan biri olmalıdır (GEN-07). */
@Service
public class CreateShipmentService implements CreateShipmentUseCase {

    private static final String RESOURCE_TYPE = "Shipment";

    private final ShipmentCreationPort creationPort;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public CreateShipmentService(ShipmentCreationPort creationPort, ScopeGuard scopeGuard, Clock clock) {
        this.creationPort = creationPort;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedShipment createShipment(CreateShipmentCommand command) {
        Actor actor = command.actor();
        NewShipment shipment =
                new NewShipment(command.siteId(), command.parties(), command.vehicleId(), clock.instant());
        ScopedResource target = new ScopedResource(
                RESOURCE_TYPE,
                UUID.randomUUID(),
                shipment.siteId(),
                command.parties().subcontractorId(),
                null);
        scopeGuard.validateInScope(actor, target, Permission.SHIPMENT_CREATE.name());
        return creationPort.create(shipment, actor.userId());
    }
}
