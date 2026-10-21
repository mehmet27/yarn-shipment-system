package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Şoförün yükü teslim alma beyanı.
 *
 * @param actor beyan eden şoför (SEC-04)
 * @param shipmentId sevkiyat
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 */
public record ConfirmPickupCommand(Actor actor, UUID shipmentId, UUID idempotencyKey) {

    public ConfirmPickupCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
