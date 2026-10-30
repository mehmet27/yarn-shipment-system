package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** İtiraz ve kontrol satırlarını domain modeline çevirir. */
final class ClaimRows {

    private ClaimRows() {}

    static Inspection mapInspection(ResultSet rs, int rowNumber) throws SQLException {
        Inspection.Request request = new Inspection.Request(
                rs.getString("request_note"), requiredUuid(rs, "created_by"), requiredInstant(rs, "selected_at"));
        return new Inspection(
                requiredUuid(rs, "id"),
                requiredUuid(rs, "shipment_id"),
                requiredUuid(rs, "discrepancy_id"),
                InspectionKind.valueOf(rs.getString("kind")),
                request,
                result(rs));
    }

    static UUID requiredUuid(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getObject(column, UUID.class), column);
    }

    static @Nullable UUID optionalUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    static Instant requiredInstant(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(column), column).toInstant();
    }

    private static Inspection.@Nullable Result result(ResultSet rs) throws SQLException {
        String summary = rs.getString("result");
        if (summary == null) {
            return null;
        }
        return new Inspection.Result(
                summary,
                optionalUuid(rs, "result_evidence_id"),
                requiredUuid(rs, "performed_by"),
                requiredInstant(rs, "performed_at"));
    }
}
