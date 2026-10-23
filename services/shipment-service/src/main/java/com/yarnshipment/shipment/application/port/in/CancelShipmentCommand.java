package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyat iptal isteği.
 *
 * @param actor iptal eden kullanıcı
 * @param shipmentId sevkiyat
 * @param reason iptal gerekçesi
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 */
public record CancelShipmentCommand(Actor actor, UUID shipmentId, String reason, UUID idempotencyKey) {

    public CancelShipmentCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
