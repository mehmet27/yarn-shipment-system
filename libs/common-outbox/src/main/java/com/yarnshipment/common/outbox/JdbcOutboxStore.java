package com.yarnshipment.common.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Servisin kendi {@code outbox_event} tablosu (referans şema: shipment-service ortak nesneler migration'ı). */
public final class JdbcOutboxStore implements OutboxStore {

    private static final TypeReference<Map<String, String>> HEADERS = new TypeReference<>() {};
    private static final String SELECT_UNPUBLISHED = """
            SELECT id, created_at, aggregate_id, event_type, payload::text AS payload, headers::text AS headers,
                   occurred_at
              FROM outbox_event
             WHERE published_at IS NULL
             ORDER BY created_at, id
             LIMIT :limit
               FOR UPDATE SKIP LOCKED""";
    private static final String MARK_PUBLISHED =
            "UPDATE outbox_event SET published_at = :at WHERE id = :id AND created_at = :createdAt";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public JdbcOutboxStore(JdbcClient jdbc, JsonMapper jsonMapper, Clock clock) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public List<OutboxEvent> lockUnpublished(int limit) {
        return jdbc.sql(SELECT_UNPUBLISHED)
                .param("limit", limit)
                .query((rs, rowNum) -> toEvent(rs))
                .list();
    }

    @Override
    public void markPublished(List<OutboxEvent> events) {
        Timestamp at = Timestamp.from(clock.instant());
        for (OutboxEvent event : events) {
            jdbc.sql(MARK_PUBLISHED)
                    .param("at", at)
                    .param("id", event.id())
                    .param("createdAt", Timestamp.from(event.createdAt()))
                    .update();
        }
    }

    private OutboxEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("event_type"),
                rs.getString("payload"),
                jsonMapper.readValue(rs.getString("headers"), HEADERS),
                rs.getTimestamp("occurred_at").toInstant());
    }
}
