package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.ResetCodePort;
import com.yarnshipment.shipment.masterdata.domain.model.ResetCode;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ResetCodePort}'un JDBC uygulaması; kullanıcı başına tek satır, yenisi eskisini değiştirir. */
@Component
final class ResetCodeAdapter implements ResetCodePort {

    private final JdbcClient jdbc;

    ResetCodeAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(ResetCode code) {
        jdbc.sql("""
                INSERT INTO md_password_reset_code (user_id, code_hash, salt, issued_at, expires_at, attempts)
                VALUES (:userId, :hash, :salt, :issuedAt, :expiresAt, 0)
                ON CONFLICT (user_id) DO UPDATE
                SET code_hash = EXCLUDED.code_hash, salt = EXCLUDED.salt, issued_at = EXCLUDED.issued_at,
                    expires_at = EXCLUDED.expires_at, attempts = 0
                """)
                .param("userId", code.userId())
                .param("hash", code.codeHash())
                .param("salt", code.salt())
                .param("issuedAt", Timestamp.from(code.issuedAt()))
                .param("expiresAt", Timestamp.from(code.expiresAt()))
                .update();
    }

    @Override
    public Optional<ResetCode> find(UUID userId) {
        return jdbc.sql("""
                SELECT user_id, code_hash, salt, issued_at, expires_at, attempts
                FROM md_password_reset_code WHERE user_id = :userId
                """)
                .param("userId", userId)
                .query((rs, row) -> new ResetCode(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("code_hash"),
                        rs.getString("salt"),
                        rs.getTimestamp("issued_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getInt("attempts")))
                .optional();
    }

    @Override
    public void recordFailedAttempt(UUID userId) {
        jdbc.sql("UPDATE md_password_reset_code SET attempts = attempts + 1 WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    @Override
    public void delete(UUID userId) {
        jdbc.sql("DELETE FROM md_password_reset_code WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }
}
