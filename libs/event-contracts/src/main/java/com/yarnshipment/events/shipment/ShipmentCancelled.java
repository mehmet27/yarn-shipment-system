package com.yarnshipment.events.shipment;

import java.time.Instant;
import java.util.UUID;

/**
 * SevkiyatIptalEdildi: sevkiyat iptal edildi. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param cancelledAt iptal zamanı
 */
public record ShipmentCancelled(UUID shipmentId, UUID siteId, Instant cancelledAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-cancelled.v1";
}
