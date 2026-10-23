package com.yarnshipment.shipment.domain.cancellation;

import java.time.Instant;
import java.util.UUID;

/**
 * Sevkiyat iptal edildi (olay kataloğu: SevkiyatIptalEdildi). Serbest metin gerekçe olaya konmaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param cancelledAt iptal zamanı
 */
public record ShipmentCancelled(UUID shipmentId, UUID siteId, Instant cancelledAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-cancelled.v1";

    public static ShipmentCancelled of(Cancellation cancellation) {
        return new ShipmentCancelled(cancellation.shipmentId(), cancellation.siteId(), cancellation.cancelledAt());
    }
}
