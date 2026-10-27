package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotOpenException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link DiscrepancyDecisionPort}'un JDBC uygulaması. Güncelleme yalnızca hâlâ çözülmemiş satırı etkiler; böylece iki
 * kişi aynı anda karar verirse ikincisi çakışma alır. Geçmiş kaydı ve olay aynı transaction'da yazılır (EVT-03).
 */
@Component
final class DiscrepancyDecisionAdapter implements DiscrepancyDecisionPort {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String UPDATE_ASSIGNMENT = """
            UPDATE discrepancy
               SET status = 'IN_REVIEW', assignee_id = :assignee, updated_at = :at, updated_by = :actor,
                   version = version + 1
             WHERE id = :id AND status IN ('OPEN', 'IN_REVIEW')""";
    private static final String UPDATE_RESOLUTION = """
            UPDATE discrepancy
               SET status = 'RESOLVED', resolution = :resolution, resolution_note = :note,
                   liable_party = :liableParty, resolved_by = :actor, resolved_at = :at, updated_at = :at,
                   updated_by = :actor, version = version + 1
             WHERE id = :id AND status IN ('OPEN', 'IN_REVIEW')""";
    private static final String COUNT_UNRESOLVED = """
            SELECT count(*) FROM discrepancy WHERE shipment_id = :shipmentId AND status IN ('OPEN', 'IN_REVIEW')""";
    private static final String INSERT_ACTIVITY = """
            INSERT INTO discrepancy_activity (id, created_at, discrepancy_id, action_type, note, actor_id, occurred_at)
            VALUES (:id, :at, :discrepancyId, :action, :note, :actor, :at)""";
    private static final String INSERT_OUTBOX = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (:id, :at, :aggregateType, :shipmentId, :type, :payload::jsonb, :headers::jsonb, :at)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    DiscrepancyDecisionAdapter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    @Override
    public void saveAssignment(Discrepancy assigned, UUID actorId, Instant at) {
        Timestamp timestamp = Timestamp.from(at);
        Stamp stamp = new Stamp(actorId, timestamp);
        setActor(actorId);
        int updated = jdbc.sql(UPDATE_ASSIGNMENT)
                .param("id", assigned.id())
                .param("assignee", assigned.handling().assigneeId())
                .param("at", timestamp)
                .param("actor", actorId)
                .update();
        requireUpdated(updated, assigned);
        insertActivity(
                assigned.id(), "ASSIGNED", String.valueOf(assigned.handling().assigneeId()), stamp);
    }

    @Override
    public void saveResolution(Discrepancy resolved, UUID actorId, Instant at) {
        Discrepancy.Decision decision = resolved.decision();
        if (decision == null) {
            throw new IllegalArgumentException("Resolved discrepancy without decision id=" + resolved.id());
        }
        Timestamp timestamp = Timestamp.from(at);
        Stamp stamp = new Stamp(actorId, timestamp);
        setActor(actorId);
        int updated = jdbc.sql(UPDATE_RESOLUTION)
                .param("id", resolved.id())
                .param("resolution", decision.resolution().name())
                .param("note", decision.note())
                .param("liableParty", decision.liableParty())
                .param("at", timestamp)
                .param("actor", actorId)
                .update();
        requireUpdated(updated, resolved);
        insertActivity(resolved.id(), "RESOLVED", decision.note(), stamp);
        int remaining = jdbc.sql(COUNT_UNRESOLVED)
                .param("shipmentId", resolved.shipmentId())
                .query(Integer.class)
                .single();
        publish(resolved, remaining, stamp);
    }

    private void setActor(UUID actorId) {
        jdbc.sql(SET_ACTOR).param("actor", actorId.toString()).query().singleValue();
    }

    /** Satır güncellenmediyse uyuşmazlık okunduktan sonra başkası tarafından çözülmüştür. */
    private static void requireUpdated(int updated, Discrepancy discrepancy) {
        if (updated == 0) {
            throw new DiscrepancyNotOpenException(discrepancy.id(), DiscrepancyStatus.RESOLVED);
        }
    }

    private void insertActivity(UUID discrepancyId, String action, String note, Stamp stamp) {
        jdbc.sql(INSERT_ACTIVITY)
                .param("id", ids.next())
                .param("at", stamp.at())
                .param("discrepancyId", discrepancyId)
                .param("action", action)
                .param("note", note)
                .param("actor", stamp.actorId())
                .update();
    }

    private void publish(Discrepancy resolved, int remaining, Stamp stamp) {
        Discrepancy.Decision decision = Objects.requireNonNull(resolved.decision());
        DiscrepancyResolved event = new DiscrepancyResolved(
                resolved.id(),
                resolved.discrepancyNo(),
                resolved.shipmentId(),
                resolved.siteId(),
                decision.resolution().name(),
                decision.resolvedAt(),
                remaining);
        jdbc.sql(INSERT_OUTBOX)
                .param("id", ids.next())
                .param("at", stamp.at())
                .param("aggregateType", AGGREGATE_TYPE)
                .param("shipmentId", resolved.shipmentId())
                .param("type", DiscrepancyResolved.TYPE)
                .param("payload", jsonMapper.writeValueAsString(event))
                .param(
                        "headers",
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", stamp.actorId().toString())))
                .update();
    }

    /** Yazan kişi ve zaman; her yazım satırında aynıdır. */
    private record Stamp(UUID actorId, Timestamp at) {}
}
