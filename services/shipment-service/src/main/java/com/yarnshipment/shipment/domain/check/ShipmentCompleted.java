package com.yarnshipment.shipment.domain.check;

import java.time.Instant;
import java.util.UUID;

/**
 * Teslim kontrolü uyuşmazlıksız bitti (olay kataloğu: SevkiyatTamamlandi). Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param completedAt tamamlanma zamanı
 */
public record ShipmentCompleted(UUID shipmentId, UUID siteId, Instant completedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-completed.v1";

    public static ShipmentCompleted of(CheckConclusion conclusion) {
        return new ShipmentCompleted(conclusion.shipmentId(), conclusion.siteId(), conclusion.concludedAt());
    }
}
