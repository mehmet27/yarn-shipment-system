package com.yarnshipment.shipment.application.port.in.fabric;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Bir sevkiyatın kumaş girişi ya da iplik dönüşü kayıtlarını okuma isteği.
 *
 * @param actor isteyen (SEC-04)
 * @param shipmentId sevkiyat
 */
public record ShipmentRecordsQuery(Actor actor, UUID shipmentId) {

    public ShipmentRecordsQuery {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
    }
}
