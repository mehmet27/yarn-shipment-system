package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyat geçmişi isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param shipmentId sevkiyat kimliği
 */
public record GetShipmentHistoryQuery(Actor actor, UUID shipmentId) {

    public GetShipmentHistoryQuery {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
    }
}
