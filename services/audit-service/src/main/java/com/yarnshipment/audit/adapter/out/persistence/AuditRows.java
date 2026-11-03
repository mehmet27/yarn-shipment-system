package com.yarnshipment.audit.adapter.out.persistence;

import com.yarnshipment.audit.domain.model.AuditRecord;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** audit tablolarından okunan satırların domain modeline çevrimi. */
final class AuditRows {

    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

    private AuditRows() {}

    static AuditRecord mapRecord(ResultSet rs, JsonMapper jsonMapper) throws SQLException {
        UUID actorUserId = rs.getObject("actor_user_id", UUID.class);
        UUID entityId = rs.getObject("entity_id", UUID.class);
        String entityType = rs.getString("entity_type");
        return new AuditRecord(
                rs.getLong("id"),
                instant(rs, "occurred_at"),
                instant(rs, "recorded_at"),
                new AuditRecord.Origin(
                        Objects.requireNonNull(rs.getString("source_service")),
                        Objects.requireNonNull(rs.getObject("event_id", UUID.class)),
                        rs.getString("trace_id")),
                new AuditRecord.Actor(actorUserId, rs.getString("actor_role"), rs.getString("client_id")),
                Objects.requireNonNull(rs.getString("action")),
                rs.getString("decision"),
                entityType == null ? null : new AuditRecord.Entity(entityType, entityId),
                json(jsonMapper, rs.getString("before_json")),
                json(jsonMapper, rs.getString("after_json")));
    }

    static ChainCheck mapCheck(ResultSet rs, int rowNum) throws SQLException {
        long broken = rs.getLong("broken_at_id");
        Long brokenAtId = rs.wasNull() ? null : broken;
        return new ChainCheck(
                rs.getLong("id"),
                instant(rs, "checked_at"),
                rs.getLong("from_id"),
                rs.getLong("to_id"),
                rs.getBoolean("ok"),
                brokenAtId);
    }

    /** JSON nesnesi olmayan değer (dizi, sayı) okuma modelinde yer almaz; olay gövdeleri her zaman nesnedir. */
    private static @Nullable Map<String, Object> json(JsonMapper jsonMapper, @Nullable String value) {
        return value == null ? null : jsonMapper.readValue(value, JSON_OBJECT);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(column), column).toInstant();
    }
}
