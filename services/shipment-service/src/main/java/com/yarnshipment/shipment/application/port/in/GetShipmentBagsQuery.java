package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyat çuvalları isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param shipmentId sevkiyat kimliği
 */
public record GetShipmentBagsQuery(Actor actor, UUID shipmentId) {

    public GetShipmentBagsQuery {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
    }
}
