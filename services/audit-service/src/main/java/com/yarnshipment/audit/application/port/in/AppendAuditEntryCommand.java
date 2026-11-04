package com.yarnshipment.audit.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Zincire eklenecek bir olay. Aktör kullanıcısızsa servis hesabı ({@code clientId}) dolu olmalıdır.
 *
 * @param eventId CloudEvents {@code id}; aynı olay iki kez yazılmaz
 * @param occurredAt olayın gerçekleştiği zaman
 * @param sourceService olayı üreten servis
 * @param actor işlemi yapan
 * @param action olay türü
 * @param entityType etkilenen kayıt türü
 * @param entityId etkilenen kayıt kimliği
 * @param afterJson olay gövdesi (JSON)
 * @param traceId korelasyon kimliği
 */
public record AppendAuditEntryCommand(
        UUID eventId,
        Instant occurredAt,
        String sourceService,
        Actor actor,
        String action,
        @Nullable String entityType,
        @Nullable UUID entityId,
        @Nullable String afterJson,
        @Nullable String traceId) {

    public AppendAuditEntryCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(sourceService, "sourceService");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(action, "action");
        if (actor.userId() == null && actor.clientId() == null) {
            throw new IllegalArgumentException("Audit entry needs actorUserId or clientId eventId=" + eventId);
        }
    }

    /**
     * İşlemi yapan.
     *
     * @param userId Keycloak kullanıcı kimliği
     * @param role kullanıcının rolleri
     * @param clientId kullanıcısız işte servis hesabı
     */
    public record Actor(
            @Nullable UUID userId,
            @Nullable String role,
            @Nullable String clientId) {}
}
