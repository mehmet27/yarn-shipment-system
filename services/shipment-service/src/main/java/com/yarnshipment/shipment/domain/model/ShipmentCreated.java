package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Sevkiyat taslağı oluşturuldu (olay kataloğu: SevkiyatOlusturuldu); kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param shipmentNo insan okunur numara
 * @param siteId çıkış lokasyonu
 * @param subcontractorId fasoncu
 * @param createdAt oluşturma zamanı
 */
public record ShipmentCreated(
        UUID shipmentId, String shipmentNo, UUID siteId, UUID subcontractorId, Instant createdAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-created.v1";
}
