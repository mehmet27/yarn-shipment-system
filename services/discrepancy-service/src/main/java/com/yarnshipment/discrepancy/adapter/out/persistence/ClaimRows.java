package com.yarnshipment.discrepancy.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** İtiraz satırlarını domain modeline çevirir. */
final class ClaimRows {

    private ClaimRows() {}

    static UUID requiredUuid(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getObject(column, UUID.class), column);
    }

    static @Nullable UUID optionalUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    static Instant requiredInstant(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(column), column).toInstant();
    }
}
