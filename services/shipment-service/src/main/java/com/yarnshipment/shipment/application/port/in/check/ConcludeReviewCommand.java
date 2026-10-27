package com.yarnshipment.shipment.application.port.in.check;

import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyatın tüm uyuşmazlıkları çözüldü olayı.
 *
 * @param eventId CloudEvents kimliği; tekrar gelen olayı ayırt eder (EVT-05)
 * @param eventType CloudEvents türü
 * @param shipmentId sevkiyat
 */
public record ConcludeReviewCommand(UUID eventId, String eventType, UUID shipmentId) {

    public ConcludeReviewCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(shipmentId, "shipmentId");
    }
}
