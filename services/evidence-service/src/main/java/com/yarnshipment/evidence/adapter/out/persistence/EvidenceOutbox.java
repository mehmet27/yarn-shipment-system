package com.yarnshipment.evidence.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Kanıt olaylarını, çağıranın transaction'ı içinde outbox tablosuna yazar (EVT-03). */
@Component
final class EvidenceOutbox {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String INSERT = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (:id, :at, :aggregateType, :shipmentId, :type, :payload::jsonb, :headers::jsonb, :at)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    EvidenceOutbox(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    /** Olayın hangi sevkiyata, kim tarafından ve ne zaman yazıldığı. */
    record Origin(UUID shipmentId, UUID actorId, Instant at) {}

    void publish(Origin origin, String type, Object event) {
        jdbc.sql(INSERT)
                .param("id", ids.next())
                .param("at", Timestamp.from(origin.at()))
                .param("aggregateType", AGGREGATE_TYPE)
                .param("shipmentId", origin.shipmentId())
                .param("type", type)
                .param("payload", jsonMapper.writeValueAsString(event))
                .param(
                        "headers",
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", origin.actorId().toString())))
                .update();
    }
}
