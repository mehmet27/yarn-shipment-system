package com.yarnshipment.discrepancy.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Oluşturulan sevkiyat ({@code ShipmentCreated}).
 *
 * @param shipmentId sevkiyat
 * @param shipmentNo sevkiyat numarası
 * @param siteId çıkış lokasyonu
 * @param subcontractorId fasoncu
 * @param createdAt oluşturma zamanı
 */
public record RegisterShipmentCommand(
        UUID shipmentId, String shipmentNo, UUID siteId, UUID subcontractorId, Instant createdAt) {

    public RegisterShipmentCommand {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(shipmentNo, "shipmentNo");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
