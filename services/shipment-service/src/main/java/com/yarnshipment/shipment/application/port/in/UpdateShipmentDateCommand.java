package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Taslak sevkiyatın gününü değiştirme isteği.
 *
 * @param actor giriş yapan kullanıcı
 * @param shipmentId sevkiyat kimliği
 * @param shipmentDate yeni gün; bugün ya da ileri bir gün (Türkiye saati)
 */
public record UpdateShipmentDateCommand(Actor actor, UUID shipmentId, LocalDate shipmentDate) {

    public UpdateShipmentDateCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(shipmentDate, "shipmentDate");
    }
}
