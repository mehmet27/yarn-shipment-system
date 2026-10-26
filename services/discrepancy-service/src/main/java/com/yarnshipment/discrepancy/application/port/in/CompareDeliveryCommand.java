package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import java.util.Objects;
import java.util.UUID;

/**
 * Bir teslim girişinin karşılaştırılması.
 *
 * @param eventId tetikleyen olayın kimliği; idempotency anahtarı (EVT-05)
 * @param eventType tetikleyen olayın tipi
 * @param actorId teslim girişini yapan kullanıcı; kayıtların {@code created_by} değeri
 * @param input iki tarafın değerleri
 */
public record CompareDeliveryCommand(UUID eventId, String eventType, UUID actorId, ComparisonInput input) {

    public CompareDeliveryCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(input, "input");
    }
}
