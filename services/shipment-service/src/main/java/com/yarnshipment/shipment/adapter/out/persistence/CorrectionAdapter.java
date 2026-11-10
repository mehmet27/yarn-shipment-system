package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.in.correction.ShipmentCorrection;
import com.yarnshipment.shipment.application.port.out.CorrectionPort;
import com.yarnshipment.shipment.domain.correction.CorrectedValue;
import com.yarnshipment.shipment.domain.correction.CorrectedValue.Weight;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.CorrectionConflictException;
import com.yarnshipment.shipment.domain.correction.CorrectionTarget;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link CorrectionPort}'un JDBC uygulaması. Kilitli sevkiyatın tartım satırlarına yalnızca
 * {@code app.correction_record_id} ayarlıyken yazılabilir (kilit tetikleyicisi); ayar transaction'a özeldir.
 */
@Component
final class CorrectionAdapter implements CorrectionPort {

    private static final String SELECT = """
            SELECT id, shipment_id, field_name, old_value, new_value, reason, status, requested_by, approved_by,
                   created_at
            FROM correction_record""";
    private static final String INSERT_RECORD = """
            INSERT INTO correction_record (id, shipment_id, entity_type, entity_id, field_name, old_value, new_value,
                                           reason, status, requested_by, approved_by, approved_at, created_at,
                                           created_by)
            VALUES (:id, :shipmentId, :entityType, :entityId, :field, :old, :new, :reason, 'APPROVED', :actor, :actor,
                    :at, :at, :actor)""";
    private static final String SET_CORRECTION = "SELECT set_config('app.correction_record_id', ?, true)";
    private static final String SUPERSEDE_WEIGHING = """
            UPDATE weighing SET superseded_by = :newId, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :oldId""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, scale_ticket_no, weighed_at,
                                  entered_by, created_at, created_by, expected_tare_kg, tare_tolerance_pct)
            SELECT :newId, shipment_id, side, :gross, :tare, bag_count, scale_ticket_no, weighed_at, entered_by, :at,
                   :actor, expected_tare_kg, tare_tolerance_pct
            FROM weighing WHERE id = :oldId""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    CorrectionAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public Optional<ShipmentCorrection> findCorrection(UUID correctionId) {
        return jdbc.sql(SELECT + " WHERE id = ?")
                .param(correctionId)
                .query((rs, rowNum) -> map(rs))
                .optional();
    }

    @Override
    public List<ShipmentCorrection> findCorrections(UUID shipmentId) {
        return jdbc.sql(SELECT + " WHERE shipment_id = ? ORDER BY created_at, id")
                .param(shipmentId)
                .query((rs, rowNum) -> map(rs))
                .list();
    }

    @Override
    public void recordCorrection(Correction correction, UUID actorUserId) {
        RowChange change = new RowChange(
                lockCurrentRow(correction),
                transitions.nextId(),
                actorUserId,
                Timestamp.from(correction.correctedAt()));
        transitions.actAs(actorUserId);
        insertRecord(correction, change);
        jdbc.sql(SET_CORRECTION).param(correction.id().toString()).query().singleValue();
        insertNewRow(correction, change);
        change.bind(jdbc.sql(SUPERSEDE_WEIGHING)).update();
    }

    /** Onaylanmış kayıt: yalnızca Admin düzeltebildiği için isteyen ve onaylayan aynı kişidir. */
    private void insertRecord(Correction correction, RowChange change) {
        jdbc.sql(INSERT_RECORD)
                .param("id", correction.id())
                .param("shipmentId", correction.shipmentId())
                .param("entityType", correction.target().entityType())
                .param("entityId", change.oldId())
                .param("field", correction.target().name())
                .param("old", correction.oldValue().text())
                .param("new", correction.newValue().text())
                .param("reason", correction.reason())
                .param("actor", change.actor())
                .param("at", change.at())
                .update();
    }

    /** Geçerli satırı kilitler; değeri alan modelinin gördüğü eski değer değilse araya başka düzeltme girmiştir. */
    private UUID lockCurrentRow(Correction correction) {
        CurrentRow row = jdbc.sql("SELECT id, gross_kg, tare_kg FROM weighing"
                        + " WHERE shipment_id = ? AND side = ? AND superseded_by IS NULL FOR UPDATE")
                .params(correction.shipmentId(), correction.target().side())
                .query((rs, n) -> new CurrentRow(rs.getObject("id", UUID.class), currentValue(rs)))
                .optional()
                .orElseThrow(() -> new CorrectionConflictException(correction.shipmentId(), correction.target()));
        if (!row.value().equals(correction.oldValue())) {
            throw new CorrectionConflictException(correction.shipmentId(), correction.target());
        }
        return row.id();
    }

    private static CorrectedValue currentValue(ResultSet rs) throws SQLException {
        return new Weight(rs.getBigDecimal("gross_kg"), rs.getBigDecimal("tare_kg"));
    }

    /** Yeni satır eskisinin kopyasıdır; yalnızca düzeltilen değer ve oluşturma bilgisi değişir. */
    private void insertNewRow(Correction correction, RowChange change) {
        Weight weight = (Weight) correction.newValue();
        change.bind(jdbc.sql(INSERT_WEIGHING))
                .param("gross", weight.grossKg())
                .param("tare", weight.tareKg())
                .update();
    }

    private static ShipmentCorrection map(ResultSet rs) throws SQLException {
        return new ShipmentCorrection(
                rs.getObject("id", UUID.class),
                rs.getObject("shipment_id", UUID.class),
                CorrectionTarget.valueOf(rs.getString("field_name")),
                rs.getString("old_value"),
                rs.getString("new_value"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getObject("requested_by", UUID.class),
                rs.getObject("approved_by", UUID.class),
                rs.getTimestamp("created_at").toInstant());
    }

    private record CurrentRow(UUID id, CorrectedValue value) {}

    /** Düzeltilen satır ve yerine geçen yeni satır. */
    private record RowChange(UUID oldId, UUID newId, UUID actor, Timestamp at) {

        JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement) {
            return statement
                    .param("oldId", oldId)
                    .param("newId", newId)
                    .param("actor", actor)
                    .param("at", at);
        }
    }
}
