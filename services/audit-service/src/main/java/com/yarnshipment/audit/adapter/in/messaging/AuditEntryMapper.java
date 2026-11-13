package com.yarnshipment.audit.adapter.in.messaging;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import com.yarnshipment.common.events.IncomingEvent;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Olayı denetim kaydına çevirir. Aktör ve korelasyon CloudEvents başlıklarından gelir; aktörsüz (sistem)
 * olayda kayıt, olayı üreten servisin adıyla {@code clientId} taşır.
 */
final class AuditEntryMapper {

    private static final String ACTOR_ROLES = "actorRoles";
    private static final String CORRELATION_ID = "correlationId";
    private static final Map<String, String> ENTITY_TYPES = Map.of(
            "shipment.events.v1", "Shipment",
            "evidence.events.v1", "Evidence",
            "discrepancy.events.v1", "Discrepancy",
            "stock.events.v1", "CentralStock");

    private AuditEntryMapper() {}

    static AppendAuditEntryCommand toCommand(IncomingEvent event) {
        String topic = Objects.requireNonNullElse(event.topic(), "");
        String service = serviceOf(event.source(), topic);
        String actorId = event.actorId();
        UUID actorUserId = actorId == null ? null : UUID.fromString(actorId);
        return new AppendAuditEntryCommand(
                event.id(),
                event.time(),
                service,
                new AppendAuditEntryCommand.Actor(
                        actorUserId, event.headers().get(ACTOR_ROLES), actorUserId == null ? service : null),
                event.type(),
                ENTITY_TYPES.get(topic),
                event.subject(),
                event.data(),
                event.headers().get(CORRELATION_ID));
    }

    /** {@code /yarn-shipment/discrepancy-service} kaynağından servis adını çıkarır. */
    private static String serviceOf(@Nullable String source, String fallback) {
        if (source == null || source.isBlank()) {
            return fallback;
        }
        return source.substring(source.lastIndexOf('/') + 1);
    }
}
