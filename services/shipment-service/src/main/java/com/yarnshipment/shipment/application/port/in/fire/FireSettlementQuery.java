package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Bir sevkiyatın fire dosyası üzerinde işlem isteği (okuma ve kapatma).
 *
 * @param actor isteyen (SEC-04)
 * @param shipmentId sevkiyat
 */
public record FireSettlementQuery(Actor actor, UUID shipmentId) {

    public FireSettlementQuery {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
    }
}
