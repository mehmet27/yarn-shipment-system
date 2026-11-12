package com.yarnshipment.common.outbox;

import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import java.sql.Timestamp;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Olayı çağıranın transaction'ı içinde {@code outbox_event} tablosuna yazar (EVT-03); {@link OutboxRelay} onları
 * Kafka'ya taşır. Servisler ayrı süreçte çalıştığı sürece varsayılan taşıyıcıdır.
 */
public final class OutboxDomainEventPublisher implements DomainEventPublisher {

    private static final String INSERT = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (:id, :at, :aggregateType, :aggregateId, :type, :payload::jsonb, :headers::jsonb, :at)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    public OutboxDomainEventPublisher(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void publish(DomainEvent event) {
        jdbc.sql(INSERT)
                .param("id", UuidV7.next())
                .param("at", Timestamp.from(event.occurredAt()))
                .param("aggregateType", event.aggregateType())
                .param("aggregateId", event.aggregateId())
                .param("type", event.type())
                .param("payload", jsonMapper.writeValueAsString(event.payload()))
                .param(
                        "headers",
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", event.actorId().toString())))
                .update();
    }
}
