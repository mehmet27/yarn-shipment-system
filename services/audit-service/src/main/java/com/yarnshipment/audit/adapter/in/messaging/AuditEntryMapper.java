package com.yarnshipment.audit.adapter.in.messaging;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.IncomingEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.jspecify.annotations.Nullable;

/**
 * Kafka olayını denetim kaydına çevirir. Aktör ve korelasyon CloudEvents başlıklarından gelir; aktörsüz (sistem)
 * olayda kayıt, olayı üreten servisin adıyla {@code clientId} taşır.
 */
final class AuditEntryMapper {

    private static final String ACTOR_ROLES = "actorRoles";
    private static final String CORRELATION_ID = "correlationId";
    private static final Map<String, String> ENTITY_TYPES = Map.of(
            "shipment.events.v1", "Shipment",
            "evidence.events.v1", "Evidence",
            "discrepancy.events.v1", "Discrepancy");

    private AuditEntryMapper() {}

    static AppendAuditEntryCommand toCommand(ConsumerRecord<String, String> record) {
        IncomingEvent event = CloudEventHeaders.read(record);
        String service = serviceOf(header(record, CloudEventHeaders.SOURCE), record.topic());
        String actorId = event.actorId();
        UUID actorUserId = actorId == null ? null : UUID.fromString(actorId);
        return new AppendAuditEntryCommand(
                event.id(),
                event.time(),
                service,
                new AppendAuditEntryCommand.Actor(
                        actorUserId, header(record, ACTOR_ROLES), actorUserId == null ? service : null),
                event.type(),
                ENTITY_TYPES.get(record.topic()),
                event.subject(),
                event.data(),
                header(record, CORRELATION_ID));
    }

    /** {@code /yarn-shipment/discrepancy-service} kaynağından servis adını çıkarır. */
    private static String serviceOf(@Nullable String source, String fallback) {
        if (source == null || source.isBlank()) {
            return fallback;
        }
        return source.substring(source.lastIndexOf('/') + 1);
    }

    private static @Nullable String header(ConsumerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
