package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Karşılaştırma dışında açılacak uyuşmazlık.
 *
 * @param shipment lokasyon, numara ve fasoncu
 * @param finding kural ve değerler
 * @param openedAt açılış zamanı
 * @param dueAt hedef kapanış zamanı
 * @param actorId kaydın {@code created_by} değeri
 */
public record OpenedDiscrepancy(
        WatchedShipment shipment, Finding finding, Instant openedAt, Instant dueAt, UUID actorId) {

    public OpenedDiscrepancy {
        Objects.requireNonNull(shipment, "shipment");
        Objects.requireNonNull(finding, "finding");
        Objects.requireNonNull(openedAt, "openedAt");
        Objects.requireNonNull(dueAt, "dueAt");
        Objects.requireNonNull(actorId, "actorId");
    }
}
