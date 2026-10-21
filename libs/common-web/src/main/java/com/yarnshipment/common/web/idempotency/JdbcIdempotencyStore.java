package com.yarnshipment.common.web.idempotency;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Servisin kendi veritabanındaki {@code idempotency_record} tablosunu kullanır. Anahtar, tek bir {@code INSERT ...
 * ON CONFLICT DO NOTHING} ile sahiplenilir; aynı anahtarlı iki eşzamanlı istekten yalnızca biri çalışır.
 */
public final class JdbcIdempotencyStore implements IdempotencyStore {

    private static final String COMPLETED = "COMPLETED";
    private static final String IN_PROGRESS = "IN_PROGRESS";

    private final JdbcClient jdbc;
    private final Clock clock;
    private final IdempotencyProperties properties;

    public JdbcIdempotencyStore(JdbcClient jdbc, Clock clock, IdempotencyProperties properties) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    public Claim claim(String actor, String key, String fingerprint) {
        Instant now = clock.instant();
        deleteStale(actor, key, now);
        int inserted = jdbc.sql("""
                        INSERT INTO idempotency_record (actor, idempotency_key, fingerprint, state, created_at)
                        VALUES (:actor, :key, :fingerprint, 'IN_PROGRESS', :now)
                        ON CONFLICT DO NOTHING
                        """)
                .param("actor", actor)
                .param("key", key)
                .param("fingerprint", fingerprint)
                .param("now", Timestamp.from(now))
                .update();
        if (inserted == 1) {
            return new Claim.Acquired();
        }
        return existing(actor, key, fingerprint);
    }

    @Override
    public void complete(String actor, String key, StoredResponse response) {
        jdbc.sql("""
                        UPDATE idempotency_record
                           SET state = 'COMPLETED', response_status = :status, response_content_type = :contentType,
                               response_location = :location, response_body = :body
                         WHERE actor = :actor AND idempotency_key = :key
                        """)
                .param("status", response.status())
                .param("contentType", response.contentType())
                .param("location", response.location())
                .param("body", response.body())
                .param("actor", actor)
                .param("key", key)
                .update();
    }

    @Override
    public void release(String actor, String key) {
        jdbc.sql("DELETE FROM idempotency_record WHERE actor = :actor AND idempotency_key = :key")
                .param("actor", actor)
                .param("key", key)
                .update();
    }

    private void deleteStale(String actor, String key, Instant now) {
        jdbc.sql("""
                        DELETE FROM idempotency_record
                         WHERE actor = :actor AND idempotency_key = :key
                           AND ((state = 'COMPLETED' AND created_at < :completedCutoff)
                             OR (state = 'IN_PROGRESS' AND created_at < :inProgressCutoff))
                        """)
                .param("actor", actor)
                .param("key", key)
                .param("completedCutoff", Timestamp.from(now.minus(properties.completedRetention())))
                .param("inProgressCutoff", Timestamp.from(now.minus(properties.inProgressTimeout())))
                .update();
    }

    private Claim existing(String actor, String key, String fingerprint) {
        Optional<StoredRow> row = jdbc.sql("""
                        SELECT fingerprint, state, response_status, response_content_type, response_location,
                               response_body
                          FROM idempotency_record
                         WHERE actor = :actor AND idempotency_key = :key
                        """)
                .param("actor", actor)
                .param("key", key)
                .query((rs, rowNum) -> new StoredRow(
                        rs.getString("fingerprint"),
                        rs.getString("state"),
                        new StoredResponse(
                                rs.getInt("response_status"),
                                rs.getString("response_content_type"),
                                rs.getString("response_location"),
                                orEmpty(rs.getBytes("response_body")))))
                .optional();
        // Kayıt, sorgudan önce bırakılmış olabilir; istemci kısa süre sonra yeniden dener.
        if (row.isEmpty()) {
            return new Claim.InProgress();
        }
        return classify(row.get(), fingerprint);
    }

    private static Claim classify(StoredRow row, String fingerprint) {
        if (!row.fingerprint().equals(fingerprint)) {
            return new Claim.Mismatch();
        }
        return switch (row.state()) {
            case COMPLETED -> new Claim.Completed(row.response());
            case IN_PROGRESS -> new Claim.InProgress();
            default -> throw new IllegalStateException("Unknown idempotency state: " + row.state());
        };
    }

    private static byte[] orEmpty(byte @Nullable [] bytes) {
        return bytes == null ? new byte[0] : bytes;
    }

    private record StoredRow(String fingerprint, String state, StoredResponse response) {}
}
