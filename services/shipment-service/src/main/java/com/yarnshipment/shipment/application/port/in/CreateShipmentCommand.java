package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Yeni sevkiyat taslağı isteği.
 *
 * @param actor giriş yapan kullanıcı (SEC-04)
 * @param idempotencyKey istemcinin ürettiği tekrar anahtarı (API-04)
 * @param siteId çıkış lokasyonu
 * @param parties fasoncu, şoför ve plaka
 * @param vehicleId araç kimliği
 */
public record CreateShipmentCommand(
        Actor actor,
        UUID idempotencyKey,
        UUID siteId,
        ShipmentParties parties,
        @Nullable UUID vehicleId) {

    public CreateShipmentCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(parties, "parties");
    }
}
