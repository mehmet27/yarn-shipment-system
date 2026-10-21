package com.yarnshipment.fixture.clean.application.port.in;

import com.yarnshipment.fixture.clean.domain.model.Shipment;

public interface CreateShipmentUseCase {

    Shipment create(String shipmentNo);
}
