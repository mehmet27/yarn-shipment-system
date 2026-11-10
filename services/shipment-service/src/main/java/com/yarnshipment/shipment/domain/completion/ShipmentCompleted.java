package com.yarnshipment.shipment.domain.completion;

import java.time.Instant;
import java.util.UUID;

/**
 * Sevkiyat tamamlandı (olay kataloğu: SevkiyatTamamlandi). Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param completedAt tamamlanma zamanı
 */
public record ShipmentCompleted(UUID shipmentId, UUID siteId, Instant completedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-completed.v1";

    public static ShipmentCompleted of(Completion completion) {
        return new ShipmentCompleted(completion.shipmentId(), completion.siteId(), completion.completedAt());
    }
}
