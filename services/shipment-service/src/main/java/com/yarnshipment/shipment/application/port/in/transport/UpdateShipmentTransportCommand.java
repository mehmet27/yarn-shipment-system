package com.yarnshipment.shipment.application.port.in.transport;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.transport.TransportAssignment;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyatın şoför ve araç bilgisini düzeltme isteği.
 *
 * @param actor giriş yapan kullanıcı
 * @param shipmentId sevkiyat kimliği
 * @param transport yeni şoför ve/veya araç; verilmeyen bilgi değişmez
 */
public record UpdateShipmentTransportCommand(Actor actor, UUID shipmentId, TransportAssignment transport) {

    public UpdateShipmentTransportCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(transport, "transport");
    }
}
