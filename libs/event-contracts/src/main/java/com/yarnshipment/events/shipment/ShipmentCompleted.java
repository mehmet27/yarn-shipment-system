package com.yarnshipment.events.shipment;

import java.time.Instant;
import java.util.UUID;

/**
 * SevkiyatTamamlandi: irsaliye müdüre ulaştı, sevkiyat tamamlandı. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param completedAt tamamlanma zamanı
 */
public record ShipmentCompleted(UUID shipmentId, UUID siteId, Instant completedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-completed.v1";
}
