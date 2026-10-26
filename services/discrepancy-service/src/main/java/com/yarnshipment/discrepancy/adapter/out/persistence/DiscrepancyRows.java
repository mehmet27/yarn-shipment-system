package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Sonuç satırlarını domain modeline çevirir. */
final class DiscrepancyRows {

    private DiscrepancyRows() {}

    static Discrepancy map(ResultSet rs, int rowNumber) throws SQLException {
        Discrepancy.Basis basis = new Discrepancy.Basis(
                DiscrepancyType.valueOf(rs.getString("discrepancy_type")),
                Severity.valueOf(rs.getString("severity")),
                rs.getString("rule_code"));
        Discrepancy.Measure measure = new Discrepancy.Measure(
                rs.getString("expected_value"),
                rs.getString("actual_value"),
                rs.getBigDecimal("diff_kg"),
                rs.getBigDecimal("diff_pct"));
        Discrepancy.Handling handling =
                new Discrepancy.Handling(optionalUuid(rs, "assignee_id"), optionalInstant(rs, "due_at"));
        return new Discrepancy(
                requiredUuid(rs, "id"),
                rs.getString("discrepancy_no"),
                requiredUuid(rs, "site_id"),
                requiredUuid(rs, "shipment_id"),
                basis,
                measure,
                DiscrepancyStatus.valueOf(rs.getString("status")),
                handling,
                decision(rs),
                requiredInstant(rs, "created_at"));
    }

    static DiscrepancyActivity mapActivity(ResultSet rs, int rowNumber) throws SQLException {
        return new DiscrepancyActivity(
                requiredUuid(rs, "id"),
                rs.getString("action_type"),
                rs.getString("note"),
                optionalUuid(rs, "evidence_id"),
                requiredUuid(rs, "actor_id"),
                requiredInstant(rs, "occurred_at"));
    }

    private static Discrepancy.@Nullable Decision decision(ResultSet rs) throws SQLException {
        String resolution = rs.getString("resolution");
        if (resolution == null) {
            return null;
        }
        return new Discrepancy.Decision(
                Resolution.valueOf(resolution),
                rs.getString("resolution_note"),
                rs.getString("liable_party"),
                requiredUuid(rs, "resolved_by"),
                requiredInstant(rs, "resolved_at"));
    }

    private static UUID requiredUuid(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getObject(column, UUID.class), column);
    }

    private static @Nullable UUID optionalUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant requiredInstant(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(column), column).toInstant();
    }

    private static @Nullable Instant optionalInstant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
