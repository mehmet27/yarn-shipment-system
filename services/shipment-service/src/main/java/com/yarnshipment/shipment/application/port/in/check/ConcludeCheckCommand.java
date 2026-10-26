package com.yarnshipment.shipment.application.port.in.check;

import com.yarnshipment.shipment.domain.check.ComparisonOutcome;
import java.util.Objects;
import java.util.UUID;

/**
 * Karşılaştırma sonucu olayı.
 *
 * @param eventId CloudEvents kimliği; tekrar gelen olayı ayırt eder (EVT-05)
 * @param eventType CloudEvents türü
 * @param shipmentId sevkiyat
 * @param outcome karşılaştırma sonucu
 */
public record ConcludeCheckCommand(UUID eventId, String eventType, UUID shipmentId, ComparisonOutcome outcome) {

    public ConcludeCheckCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(outcome, "outcome");
    }
}
