package com.yarnshipment.moduleapi.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Bir modülün yayınladığı olay. Zarf kimliğini yayıncı atar; taşıyıcı (outbox/Kafka ya da süreç içi) üreten modülü
 * ilgilendirmez.
 *
 * @param aggregateType olayın ait olduğu kök, ör. {@code Shipment}
 * @param aggregateId kök kimliği; CloudEvents {@code subject} ve bölümleme anahtarı
 * @param type olay türü (CloudEvents {@code type}), ör. {@code ShipmentCreated.TYPE}
 * @param payload olay gövdesi; event-contracts içindeki bir kayıt
 * @param occurredAt olayın gerçekleştiği zaman
 * @param actorId olayı doğuran kullanıcı
 */
public record DomainEvent(
        String aggregateType, UUID aggregateId, String type, Object payload, Instant occurredAt, UUID actorId) {

    public DomainEvent {
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actorId, "actorId");
    }
}
