package com.yarnshipment.audit.adapter.out.persistence;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import com.yarnshipment.audit.application.port.out.AuditAppendPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * {@link AuditAppendPort}'un JDBC uygulaması. Yazma {@code audit_writer} rolü altında çalışır; rolün tablolara
 * yetkisi yoktur, ekleme yalnızca {@code audit_append(jsonb)} ile yapılır, hash zincirini ve olay tekilliğini o sağlar.
 */
@Component
final class AuditAppendAdapter implements AuditAppendPort {

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final AuditRoleScope roles;

    AuditAppendAdapter(JdbcClient jdbc, JsonMapper jsonMapper, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.roles = new AuditRoleScope(jdbc, transactionManager);
    }

    @Override
    public long append(AppendAuditEntryCommand command) {
        String entry = jsonMapper.writeValueAsString(toEntry(command));
        return roles.runAs(AuditRoleScope.WRITER, () -> jdbc.sql("SELECT audit_append(CAST(:entry AS jsonb))")
                .param("entry", entry)
                .query(Long.class)
                .single());
    }

    /** audit_log kolonlarının camelCase adlarını taşıyan nesne; boş alanlar hiç yazılmaz. */
    private ObjectNode toEntry(AppendAuditEntryCommand command) {
        ObjectNode entry = jsonMapper.createObjectNode();
        entry.put("eventId", command.eventId().toString());
        entry.put("occurredAt", command.occurredAt().toString());
        entry.put("sourceService", command.sourceService());
        entry.put("action", command.action());
        AppendAuditEntryCommand.Actor actor = command.actor();
        if (actor.userId() != null) {
            entry.put("actorUserId", actor.userId().toString());
        }
        if (actor.role() != null) {
            entry.put("actorRole", actor.role());
        }
        if (actor.clientId() != null) {
            entry.put("clientId", actor.clientId());
        }
        if (command.entityType() != null) {
            entry.put("entityType", command.entityType());
        }
        if (command.entityId() != null) {
            entry.put("entityId", command.entityId().toString());
        }
        if (command.afterJson() != null) {
            entry.set("after", jsonMapper.readTree(command.afterJson()));
        }
        if (command.traceId() != null) {
            entry.put("traceId", command.traceId());
        }
        return entry;
    }
}
