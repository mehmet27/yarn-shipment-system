package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.TicketReadingPort;
import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketFields;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketReuse;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link TicketReadingPort}'un JDBC uygulaması. Fiş numarası ve kantar adı boşluk ve harf büyüklüğü yok sayılarak
 * eşleşir; ifade {@code ticket_reading_ticket_idx} ile aynıdır.
 */
@Component
final class TicketReadingAdapter implements TicketReadingPort {

    private static final String INSERT = """
            INSERT INTO ticket_reading (evidence_id, shipment_id, evidence_type, status, gross_kg, tare_kg, net_kg,
                                        plate, weighed_at, ticket_no, scale_name, problems, read_at, created_at,
                                        created_by)
            VALUES (:evidenceId, :shipmentId, :type, :status, :grossKg, :tareKg, :netKg, :plate, :weighedAt,
                    :ticketNo, :scaleName, string_to_array(:problems, ','), :readAt, :readAt, :actor)
            ON CONFLICT (evidence_id) DO NOTHING""";
    private static final String LATEST_READ = """
            SELECT DISTINCT ON (evidence_type) evidence_id, shipment_id, evidence_type, gross_kg, tare_kg, net_kg,
                   plate, weighed_at, ticket_no, scale_name, array_to_string(problems, ',') AS problems, read_at
              FROM ticket_reading
             WHERE shipment_id = :shipmentId AND status = 'READ'
             ORDER BY evidence_type, read_at DESC, evidence_id DESC""";
    private static final String REUSES = """
            SELECT DISTINCT ON (mine.evidence_type, other.shipment_id)
                   mine.evidence_type, mine.ticket_no, other.shipment_id AS other_shipment_id
              FROM ticket_reading mine
              JOIN ticket_reading other
                ON other.shipment_id <> mine.shipment_id
               AND upper(regexp_replace(other.ticket_no, '[[:space:]]', '', 'g'))
                   = upper(regexp_replace(mine.ticket_no, '[[:space:]]', '', 'g'))
               AND upper(regexp_replace(coalesce(other.scale_name, ''), '[[:space:]]', '', 'g'))
                   = upper(regexp_replace(coalesce(mine.scale_name, ''), '[[:space:]]', '', 'g'))
             WHERE mine.evidence_id IN (:evidenceIds) AND mine.ticket_no IS NOT NULL
             ORDER BY mine.evidence_type, other.shipment_id, other.read_at""";
    private static final String SEPARATOR = ",";
    private static final String READ = "READ";
    private static final String UNREADABLE = "UNREADABLE";

    private final JdbcClient jdbc;

    TicketReadingAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveReading(TicketReading reading, UUID actorId) {
        ScaleWeights weights = reading.weights();
        TicketFields fields = reading.fields();
        jdbc.sql(INSERT)
                .param("evidenceId", reading.evidenceId())
                .param("shipmentId", reading.shipmentId())
                .param("type", reading.side().name())
                .param("status", reading.read() ? READ : UNREADABLE)
                .param("grossKg", weights == null ? null : weights.grossKg())
                .param("tareKg", weights == null ? null : weights.tareKg())
                .param("netKg", weights == null ? null : weights.netKg())
                .param("plate", fields.plate())
                .param("weighedAt", timestamp(fields.weighedAt()))
                .param("ticketNo", fields.ticketNo())
                .param("scaleName", fields.scaleName())
                .param("problems", String.join(SEPARATOR, reading.problems()))
                .param("readAt", Timestamp.from(reading.readAt()))
                .param("actor", actorId)
                .update();
    }

    @Override
    public ShipmentTickets findTickets(UUID shipmentId) {
        Map<TicketSide, TicketReading> latest =
                jdbc.sql(LATEST_READ).param("shipmentId", shipmentId).query(TicketReadingAdapter::map).stream()
                        .collect(Collectors.toMap(TicketReading::side, Function.identity()));
        if (latest.isEmpty()) {
            return ShipmentTickets.NONE;
        }
        List<TicketReuse> reuses = jdbc.sql(REUSES)
                .param(
                        "evidenceIds",
                        latest.values().stream().map(TicketReading::evidenceId).toList())
                .query((rs, row) -> new TicketReuse(
                        TicketSide.valueOf(rs.getString("evidence_type")),
                        Objects.requireNonNull(rs.getString("ticket_no")),
                        Objects.requireNonNull(rs.getObject("other_shipment_id", UUID.class))))
                .list();
        return new ShipmentTickets(
                latest.get(TicketSide.DISPATCH_SCALE), latest.get(TicketSide.DELIVERY_SCALE), reuses);
    }

    static TicketReading map(ResultSet rs, int rowNumber) throws SQLException {
        BigDecimal gross = rs.getBigDecimal("gross_kg");
        BigDecimal tare = rs.getBigDecimal("tare_kg");
        BigDecimal net = rs.getBigDecimal("net_kg");
        Timestamp weighedAt = rs.getTimestamp("weighed_at");
        String problems = rs.getString("problems");
        return new TicketReading(
                Objects.requireNonNull(rs.getObject("evidence_id", UUID.class)),
                Objects.requireNonNull(rs.getObject("shipment_id", UUID.class)),
                TicketSide.valueOf(rs.getString("evidence_type")),
                gross == null || tare == null || net == null ? null : new ScaleWeights(gross, tare, net),
                new TicketFields(
                        rs.getString("plate"),
                        weighedAt == null ? null : weighedAt.toInstant(),
                        rs.getString("ticket_no"),
                        rs.getString("scale_name")),
                problems.isEmpty() ? List.of() : Arrays.asList(problems.split(SEPARATOR, -1)),
                Objects.requireNonNull(rs.getTimestamp("read_at")).toInstant());
    }

    private static @Nullable Timestamp timestamp(@Nullable Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
