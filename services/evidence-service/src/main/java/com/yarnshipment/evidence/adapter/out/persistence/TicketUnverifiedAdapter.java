package com.yarnshipment.evidence.adapter.out.persistence;

import com.yarnshipment.evidence.application.port.out.TicketUnverifiedPort;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.ticket.TicketUnverified;
import com.yarnshipment.evidence.domain.ticket.UnverifiedReason;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link TicketUnverifiedPort}'un JDBC uygulaması. Gerekçe okumanın satırından türetilir; {@code created_by} okumayı
 * isteyen, yani fotoğrafı yükleyendir. Gerekçeyle birlikte {@code TicketUnverified} olayı yayımlanır.
 */
@Component
final class TicketUnverifiedAdapter implements TicketUnverifiedPort {

    /** Gerekçe ve FisDogrulanamadi olayı tek komutta yazılır; gerekçe zaten varsa olay da yazılmaz (EVT-03). */
    private static final String INSERT = """
            WITH unverified AS (
                INSERT INTO ticket_unverified (evidence_id, shipment_id, evidence_type, reason, declared_at,
                                               created_at, created_by)
                SELECT r.evidence_id, r.shipment_id, r.evidence_type, :reason::text, :declaredAt::timestamptz,
                       :declaredAt::timestamptz, r.created_by
                  FROM ticket_reading r WHERE r.evidence_id = :evidenceId::uuid
                ON CONFLICT (evidence_id) DO NOTHING
                RETURNING shipment_id, created_by)
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            SELECT :outboxId::uuid, :declaredAt::timestamptz, :aggregateType::text, u.shipment_id, :eventType::text,
                   :payload::jsonb, jsonb_build_object('actorId', u.created_by::text), :declaredAt::timestamptz
              FROM unverified u""";

    private static final String SELECT = """
            SELECT evidence_id, shipment_id, evidence_type, reason, declared_at
              FROM ticket_unverified WHERE evidence_id = :id""";
    private static final String AGGREGATE_TYPE = "Shipment";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    TicketUnverifiedAdapter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    @Override
    public Optional<TicketUnverified> findUnverified(UUID evidenceId) {
        return jdbc.sql(SELECT)
                .param("id", evidenceId)
                .query((rs, row) -> new TicketUnverified(
                        Objects.requireNonNull(rs.getObject("evidence_id", UUID.class)),
                        Objects.requireNonNull(rs.getObject("shipment_id", UUID.class)),
                        EvidenceType.valueOf(rs.getString("evidence_type")),
                        UnverifiedReason.valueOf(rs.getString("reason")),
                        Objects.requireNonNull(rs.getTimestamp("declared_at")).toInstant()))
                .optional();
    }

    @Override
    public void saveUnverified(TicketUnverified unverified) {
        com.yarnshipment.events.evidence.TicketUnverified event = new com.yarnshipment.events.evidence.TicketUnverified(
                unverified.evidenceId(),
                unverified.shipmentId(),
                unverified.evidenceType().name(),
                unverified.reason().name(),
                unverified.declaredAt());
        jdbc.sql(INSERT)
                .param("evidenceId", unverified.evidenceId())
                .param("reason", unverified.reason().name())
                .param("declaredAt", Timestamp.from(unverified.declaredAt()))
                .param("outboxId", ids.next())
                .param("aggregateType", AGGREGATE_TYPE)
                .param("eventType", com.yarnshipment.events.evidence.TicketUnverified.TYPE)
                .param("payload", jsonMapper.writeValueAsString(event))
                .update();
    }
}
