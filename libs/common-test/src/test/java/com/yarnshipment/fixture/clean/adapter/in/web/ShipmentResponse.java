package com.yarnshipment.fixture.clean.adapter.in.web;

import com.yarnshipment.fixture.clean.domain.model.Shipment;

record ShipmentResponse(String shipmentNo) {

    static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(shipment.shipmentNo());
    }
}
