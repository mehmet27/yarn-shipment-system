package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.adapter.in.web.api.model.CreateShipmentRequest;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import java.util.UUID;

/** Sevkiyat oluşturma isteğini uygulama komutuna çevirir. */
final class ShipmentCreations {

    private ShipmentCreations() {}

    static CreateShipmentCommand toCommand(Actor actor, UUID idempotencyKey, CreateShipmentRequest request) {
        ShipmentParties parties = new ShipmentParties(
                request.getSubcontractorId(),
                request.getSubcontractorTitle(),
                request.getDriverId(),
                request.getDriverName(),
                request.getPlateNo());
        return new CreateShipmentCommand(actor, idempotencyKey, request.getSiteId(), parties, request.getVehicleId());
    }
}
