package com.yarnshipment.discrepancy.domain.rule;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyat olaylarından izlenen sevkiyat: karşılaştırma dışında açılan uyuşmazlıklar (fotoğraf, zaman) lokasyon,
 * numara ve fasoncuyu buradan alır.
 *
 * @param shipmentId sevkiyat
 * @param shipmentNo sevkiyat numarası
 * @param siteId çıkış lokasyonu
 * @param subcontractorId fasoncu
 * @param dispatchedAt yola çıkış; henüz çıkmadıysa boş
 */
public record WatchedShipment(
        UUID shipmentId,
        String shipmentNo,
        UUID siteId,
        @Nullable UUID subcontractorId,
        @Nullable Instant dispatchedAt) {

    public WatchedShipment {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(shipmentNo, "shipmentNo");
        Objects.requireNonNull(siteId, "siteId");
    }
}
