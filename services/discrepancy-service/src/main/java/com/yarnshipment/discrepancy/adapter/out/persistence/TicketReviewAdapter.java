package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.TicketReviewPort;
import com.yarnshipment.discrepancy.domain.ticket.EnteredNets;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link TicketReviewPort}'un JDBC uygulaması. "AI yanıldı" işareti uyuşmazlık geçmişine {@code TICKET_MISREAD} olarak,
 * okunan fotoğrafın kimliğiyle aynı transaction'da yazılır.
 */
@Component
final class TicketReviewAdapter implements TicketReviewPort {

    private static final String READINGS = """
            SELECT evidence_id, shipment_id, evidence_type, gross_kg, tare_kg, net_kg, plate, weighed_at, ticket_no,
                   scale_name, array_to_string(problems, ',') AS problems, read_at
              FROM ticket_reading
             WHERE shipment_id = :shipmentId
             ORDER BY read_at DESC, evidence_id DESC""";
    private static final String ENTERED = """
            SELECT dispatch_net_kg, delivery_net_kg FROM comparison_input WHERE shipment_id = :shipmentId""";
    private static final String MISREADS = """
            SELECT m.evidence_id, m.discrepancy_id, m.created_by, m.marked_at, m.note
              FROM ticket_misread m JOIN ticket_reading r ON r.evidence_id = m.evidence_id
             WHERE r.shipment_id = :shipmentId""";
    private static final String INSERT = """
            INSERT INTO ticket_misread (evidence_id, discrepancy_id, note, marked_at, created_at, created_by)
            VALUES (:evidenceId, :discrepancyId, :note, :at, :at, :actor)
            ON CONFLICT (evidence_id) DO NOTHING""";
    private static final String INSERT_ACTIVITY = """
            INSERT INTO discrepancy_activity (id, created_at, discrepancy_id, action_type, note, evidence_id, actor_id,
                                              occurred_at)
            VALUES (:id, :at, :discrepancyId, 'TICKET_MISREAD', :note, :evidenceId, :actor, :at)""";

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;

    TicketReviewAdapter(JdbcClient jdbc, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public List<TicketReading> listReadings(UUID shipmentId) {
        return jdbc.sql(READINGS)
                .param("shipmentId", shipmentId)
                .query(TicketReadingAdapter::map)
                .list();
    }

    @Override
    public Optional<EnteredNets> findEnteredNets(UUID shipmentId) {
        return jdbc.sql(ENTERED)
                .param("shipmentId", shipmentId)
                .query((rs, row) -> new EnteredNets(
                        Objects.requireNonNull(rs.getBigDecimal("dispatch_net_kg")),
                        Objects.requireNonNull(rs.getBigDecimal("delivery_net_kg"))))
                .optional();
    }

    @Override
    public List<TicketMisread> findMisreads(UUID shipmentId) {
        return jdbc.sql(MISREADS)
                .param("shipmentId", shipmentId)
                .query((rs, row) -> new TicketMisread(
                        Objects.requireNonNull(rs.getObject("evidence_id", UUID.class)),
                        Objects.requireNonNull(rs.getObject("discrepancy_id", UUID.class)),
                        Objects.requireNonNull(rs.getObject("created_by", UUID.class)),
                        Objects.requireNonNull(rs.getTimestamp("marked_at")).toInstant(),
                        rs.getString("note")))
                .list();
    }

    @Override
    public void saveMisread(TicketMisread misread) {
        Timestamp at = Timestamp.from(misread.markedAt());
        int inserted = jdbc.sql(INSERT)
                .param("evidenceId", misread.evidenceId())
                .param("discrepancyId", misread.discrepancyId())
                .param("note", misread.note())
                .param("at", at)
                .param("actor", misread.markedBy())
                .update();
        if (inserted == 0) {
            return;
        }
        jdbc.sql(INSERT_ACTIVITY)
                .param("id", ids.next())
                .param("at", at)
                .param("discrepancyId", misread.discrepancyId())
                .param("note", misread.note())
                .param("evidenceId", misread.evidenceId())
                .param("actor", misread.markedBy())
                .update();
    }
}
