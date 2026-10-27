package com.yarnshipment.evidence.adapter.out.persistence;

import com.yarnshipment.events.evidence.TicketRead;
import com.yarnshipment.evidence.application.port.out.TicketReadingStorePort;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.ticket.ReadWeights;
import com.yarnshipment.evidence.domain.ticket.ReaderVersion;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketFields;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketReadingStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link TicketReadingStorePort}'un JDBC uygulaması. Aynı kanıt için eşzamanlı iki okuma yarışırsa ilk yazılan kalır;
 * kanıtı yükleyen okumayı istediği için {@code created_by} yükleyendir. Okumayla birlikte {@code TicketRead} olayı
 * yayımlanır; karşılaştırmayı discrepancy-service yapar.
 */
@Component
final class TicketReadingAdapter implements TicketReadingStorePort {

    /** Okuma ve FisOkundu olayı tek komutta yazılır; okuma zaten varsa olay da yazılmaz (EVT-03). */
    private static final String INSERT = """
            WITH reading AS (
                INSERT INTO ticket_reading (evidence_id, shipment_id, evidence_type, status, gross_kg, tare_kg, net_kg,
                                            plate, weighed_at, ticket_no, scale_name, problems, model, prompt_version,
                                            raw_response, read_at, created_at, created_by)
                SELECT :evidenceId::uuid, :shipmentId::uuid, :type::text, :status::text, :grossKg::numeric,
                       :tareKg::numeric, :netKg::numeric, :plate::text, :weighedAt::timestamptz, :ticketNo::text,
                       :scaleName::text, string_to_array(:problems::text, ','), :model::text, :promptVersion::text,
                       :rawResponse::jsonb, :readAt::timestamptz, :readAt::timestamptz, f.uploaded_by
                  FROM evidence_file f WHERE f.id = :evidenceId::uuid
                ON CONFLICT (evidence_id) DO NOTHING
                RETURNING shipment_id, created_by)
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            SELECT :outboxId::uuid, :readAt::timestamptz, :aggregateType::text, r.shipment_id, :eventType::text,
                   :payload::jsonb, jsonb_build_object('actorId', r.created_by::text), :readAt::timestamptz
              FROM reading r""";

    private static final String SELECT = """
            SELECT evidence_id, shipment_id, evidence_type, status, gross_kg, tare_kg, net_kg, plate, weighed_at,
                   ticket_no, scale_name, array_to_string(problems, ',') AS problems, model, prompt_version,
                   raw_response::text AS raw_response, read_at
              FROM ticket_reading WHERE evidence_id = :id""";
    private static final String SEPARATOR = ",";
    private static final String AGGREGATE_TYPE = "Shipment";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    TicketReadingAdapter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    @Override
    public Optional<TicketReading> findReading(UUID evidenceId) {
        return jdbc.sql(SELECT)
                .param("id", evidenceId)
                .query(TicketReadingAdapter::map)
                .optional();
    }

    @Override
    public void saveReading(TicketReading reading) {
        ReadWeights weights = reading.weights();
        TicketFields fields = reading.fields();
        jdbc.sql(INSERT)
                .param("evidenceId", reading.evidenceId())
                .param("shipmentId", reading.shipmentId())
                .param("type", reading.evidenceType().name())
                .param("status", reading.status().name())
                .param("grossKg", weights == null ? null : weights.grossKg())
                .param("tareKg", weights == null ? null : weights.tareKg())
                .param("netKg", weights == null ? null : weights.netKg())
                .param("plate", fields.plate())
                .param("weighedAt", fields.weighedAt() == null ? null : Timestamp.from(fields.weighedAt()))
                .param("ticketNo", fields.ticketNo())
                .param("scaleName", fields.scaleName())
                .param("problems", reading.problems().stream().map(Enum::name).collect(Collectors.joining(SEPARATOR)))
                .param("model", reading.version().model())
                .param("promptVersion", reading.version().promptVersion())
                .param("rawResponse", reading.rawResponse())
                .param("readAt", Timestamp.from(reading.readAt()))
                .param("outboxId", ids.next())
                .param("aggregateType", AGGREGATE_TYPE)
                .param("eventType", TicketRead.TYPE)
                .param("payload", jsonMapper.writeValueAsString(event(reading)))
                .update();
    }

    private static TicketRead event(TicketReading reading) {
        ReadWeights weights = reading.weights();
        TicketFields fields = reading.fields();
        return new TicketRead(
                reading.evidenceId(),
                reading.shipmentId(),
                reading.evidenceType().name(),
                reading.status().name(),
                weights == null ? null : new TicketRead.Weights(weights.grossKg(), weights.tareKg(), weights.netKg()),
                new TicketRead.Fields(fields.plate(), fields.weighedAt(), fields.ticketNo(), fields.scaleName()),
                reading.problems().stream().map(Enum::name).toList(),
                reading.readAt());
    }

    static TicketReading map(ResultSet rs, int rowNumber) throws SQLException {
        Timestamp weighedAt = rs.getTimestamp("weighed_at");
        BigDecimal gross = rs.getBigDecimal("gross_kg");
        BigDecimal tare = rs.getBigDecimal("tare_kg");
        return new TicketReading(
                Objects.requireNonNull(rs.getObject("evidence_id", UUID.class)),
                Objects.requireNonNull(rs.getObject("shipment_id", UUID.class)),
                EvidenceType.valueOf(rs.getString("evidence_type")),
                TicketReadingStatus.valueOf(rs.getString("status")),
                gross == null || tare == null ? null : new ReadWeights(gross, tare),
                new TicketFields(
                        rs.getString("plate"),
                        weighedAt == null ? null : weighedAt.toInstant(),
                        rs.getString("ticket_no"),
                        rs.getString("scale_name")),
                problems(rs.getString("problems")),
                new ReaderVersion(rs.getString("model"), rs.getString("prompt_version")),
                rs.getString("raw_response"),
                Objects.requireNonNull(rs.getTimestamp("read_at")).toInstant());
    }

    private static List<ReadingProblem> problems(String joined) {
        if (joined.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(joined.split(SEPARATOR, -1))
                .map(ReadingProblem::valueOf)
                .toList();
    }
}
