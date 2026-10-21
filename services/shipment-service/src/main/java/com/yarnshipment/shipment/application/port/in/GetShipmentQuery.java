package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Tek sevkiyat isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param shipmentId sevkiyat kimliği
 */
public record GetShipmentQuery(Actor actor, UUID shipmentId) {

    public GetShipmentQuery {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
    }
}
