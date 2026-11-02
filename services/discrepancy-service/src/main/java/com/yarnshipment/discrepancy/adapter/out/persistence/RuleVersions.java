package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

/** Geçerlilik aralığıyla saklanan kural satırının ortak kolonları ({@code valid_from}, {@code valid_to}). */
final class RuleVersions {

    private RuleVersions() {}

    static <T> SettingVersion<T> read(ResultSet rs, T value) throws SQLException {
        Timestamp validTo = rs.getTimestamp("valid_to");
        return new SettingVersion<>(
                value,
                rs.getTimestamp("valid_from").toInstant(),
                validTo == null ? null : validTo.toInstant(),
                rs.getObject("created_by", UUID.class));
    }
}
