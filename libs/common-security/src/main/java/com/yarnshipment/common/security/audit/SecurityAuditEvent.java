package com.yarnshipment.common.security.audit;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Güvenlik açısından önemli bir karar (yetkilendirme analizi, "Audit ve KVKK"). Kişisel veri ve token içermez
 * (SEC-12); aktör yalnızca kimliğiyle yer alır.
 *
 * @param occurredAt sunucu zamanı
 * @param actorUserId Keycloak {@code sub}
 * @param actorRoles aktörün rolleri
 * @param action yapılmak istenen işlem, ör. izin adı veya {@code GET /api/v1/shipments/{id}}
 * @param resource kaynak tipi ve kimliği, ör. {@code Shipment:0192...}
 * @param decision sonuç
 * @param reason ret nedeni, ör. {@code OUT_OF_SCOPE}
 */
public record SecurityAuditEvent(
        Instant occurredAt,
        UUID actorUserId,
        Set<String> actorRoles,
        String action,
        String resource,
        AuditDecision decision,
        String reason) {

    public SecurityAuditEvent {
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(actorUserId, "actorUserId");
        actorRoles = Set.copyOf(actorRoles);
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(reason, "reason");
    }

    /** Reddedilen erişim kaydı. */
    public static SecurityAuditEvent denied(Instant occurredAt, Actor actor, String action, String resource) {
        Set<String> roles = actor.roles().stream().map(Role::name).collect(Collectors.toSet());
        return new SecurityAuditEvent(occurredAt, actor.userId(), roles, action, resource, AuditDecision.DENIED, "");
    }

    /** Aynı olayın ret nedeni eklenmiş kopyası. */
    public SecurityAuditEvent withReason(String newReason) {
        return new SecurityAuditEvent(occurredAt, actorUserId, actorRoles, action, resource, decision, newReason);
    }
}
