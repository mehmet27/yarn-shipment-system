package com.yarnshipment.fixture.clean.adapter.in.web;

import com.yarnshipment.fixture.clean.application.port.in.CreateShipmentUseCase;

public final class ShipmentController {

    private final CreateShipmentUseCase createShipmentUseCase;

    public ShipmentController(CreateShipmentUseCase createShipmentUseCase) {
        this.createShipmentUseCase = createShipmentUseCase;
    }

    public ShipmentResponse createShipment(String shipmentNo) {
        return ShipmentResponse.from(createShipmentUseCase.create(shipmentNo));
    }
}
