package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.LotQualityLock;
import java.util.Objects;
import java.util.UUID;

/**
 * Kalitesi kilitlenen partiyi kaydetme isteği; {@code LotQualityLocked} olayından gelir.
 *
 * @param eventId olay kimliği; idempotency anahtarı (EVT-05)
 * @param eventType olay tipi
 * @param lock kilitlenen parti
 */
public record RecordLotQualityLockCommand(UUID eventId, String eventType, LotQualityLock lock) {

    public RecordLotQualityLockCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(lock, "lock");
    }
}
