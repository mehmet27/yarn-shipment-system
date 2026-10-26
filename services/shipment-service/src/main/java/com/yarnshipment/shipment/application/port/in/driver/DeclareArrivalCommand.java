package com.yarnshipment.shipment.application.port.in.driver;

import com.yarnshipment.common.security.Actor;
import java.util.Objects;
import java.util.UUID;

/**
 * Şoförün varış beyanı.
 *
 * @param actor beyan eden şoför (SEC-04)
 * @param shipmentId sevkiyat
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 * @param sealIntact varışta mühür sağlam mı
 */
public record DeclareArrivalCommand(Actor actor, UUID shipmentId, UUID idempotencyKey, boolean sealIntact) {

    public DeclareArrivalCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    }
}
