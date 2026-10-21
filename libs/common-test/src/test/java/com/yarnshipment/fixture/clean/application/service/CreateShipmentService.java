package com.yarnshipment.fixture.clean.application.service;

import com.yarnshipment.fixture.clean.application.port.in.CreateShipmentUseCase;
import com.yarnshipment.fixture.clean.domain.model.Shipment;
import java.time.Clock;

public final class CreateShipmentService implements CreateShipmentUseCase {

    private final Clock clock;

    public CreateShipmentService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Shipment create(String shipmentNo) {
        return new Shipment(shipmentNo, clock.instant());
    }
}
