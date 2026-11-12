package com.yarnshipment.audit.adapter.out.persistence;

import com.yarnshipment.audit.application.port.in.AuditRecordPage;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery.Filter;
import com.yarnshipment.audit.application.port.out.AuditQueryPort;
import com.yarnshipment.audit.domain.model.AuditRecord;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link AuditQueryPort}'un JDBC uygulaması. Okuma modeli denetim zincirinin tablolarından okur; her sorgu
 * {@code audit_reader} rolü (yalnız SELECT) altında çalışır.
 */
@Component
final class AuditQueryAdapter implements AuditQueryPort {

    private static final String SELECT = """
            SELECT id, occurred_at, recorded_at, source_service, event_id, actor_user_id, actor_role, client_id,
                   action, decision, entity_type, entity_id, before::text AS before_json, after::text AS after_json,
                   trace_id
              FROM audit_log""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final AuditRoleScope roles;

    AuditQueryAdapter(JdbcClient jdbc, JsonMapper jsonMapper, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.roles = new AuditRoleScope(jdbc, transactionManager);
    }

    @Override
    public AuditRecordPage findRecords(ListAuditRecordsQuery query) {
        return roles.runAs(AuditRoleScope.READER, () -> selectRecords(query));
    }

    private AuditRecordPage selectRecords(ListAuditRecordsQuery query) {
        Filter filter = query.filter();
        Where where = new Where();
        where.add("entity_type = :entityType", "entityType", filter.entityType());
        where.add("entity_id = :entityId", "entityId", filter.entityId());
        where.add("actor_user_id = :actorUserId", "actorUserId", filter.actorUserId());
        where.add("action = :action", "action", filter.action());
        where.add("source_service = :sourceService", "sourceService", filter.sourceService());
        where.add("occurred_at >= :from", "from", timestamp(filter.from()));
        where.add("occurred_at < :to", "to", timestamp(filter.to()));

        long total = where.bind(jdbc.sql("SELECT count(*) FROM audit_log" + where.clause()))
                .query(Long.class)
                .single();
        List<AuditRecord> items = where.bind(
                        jdbc.sql(SELECT + where.clause() + " ORDER BY id DESC LIMIT :limit OFFSET :offset"))
                .param("limit", query.size())
                .param("offset", (long) query.page() * query.size())
                .query((rs, rowNum) -> AuditRows.mapRecord(rs, jsonMapper))
                .list();
        return new AuditRecordPage(items, query.page(), query.size(), total);
    }

    @Override
    public List<ChainCheck> findLatestChainChecks(int limit) {
        return roles.runAs(AuditRoleScope.READER, () -> jdbc.sql("""
                        SELECT id, checked_at, from_id, to_id, ok, broken_at_id
                          FROM audit_chain_check
                         ORDER BY id DESC
                         LIMIT :limit""").param("limit", limit).query(AuditRows::mapCheck).list());
    }

    @Override
    public long latestRecordId() {
        return roles.runAs(AuditRoleScope.READER, () -> jdbc.sql("SELECT coalesce(max(id), 0) FROM audit_log")
                .query(Long.class)
                .single());
    }

    @Override
    public long lastVerifiedId() {
        return roles.runAs(
                AuditRoleScope.READER,
                () -> jdbc.sql("SELECT coalesce(max(to_id), 0) FROM audit_chain_check")
                        .query(Long.class)
                        .single());
    }

    private static @Nullable Timestamp timestamp(@Nullable Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    /** Dolu süzgeçleri WHERE koşuluna ve adlı parametrelere çevirir; boş süzgeç koşul eklemez. */
    private static final class Where {

        private final List<String> conditions = new ArrayList<>();
        private final Map<String, Object> params = new LinkedHashMap<>();

        void add(String condition, String name, @Nullable Object value) {
            if (value != null) {
                conditions.add(condition);
                params.put(name, value);
            }
        }

        String clause() {
            return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        }

        JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement) {
            JdbcClient.StatementSpec bound = statement;
            for (Map.Entry<String, Object> param : params.entrySet()) {
                bound = bound.param(param.getKey(), param.getValue());
            }
            return bound;
        }
    }
}
