package com.yarnshipment.evidence.adapter.out.persistence;

import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceSource;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link UploadIntentPort}'un JDBC uygulaması. */
@Component
final class UploadIntentAdapter implements UploadIntentPort {

    private static final String INSERT = """
            INSERT INTO upload_intent (id, shipment_id, site_id, evidence_type, mime_type, size_bytes, source,
                                       captured_at, storage_key, requested_by, requested_at, expires_at, created_at,
                                       created_by)
            VALUES (:id, :shipmentId, :siteId, :type, :mimeType, :sizeBytes, :source, :capturedAt, :storageKey,
                    :actor, :at, :expiresAt, :at, :actor)""";
    private static final String SELECT = """
            SELECT id, shipment_id, site_id, evidence_type, mime_type, size_bytes, source, captured_at, storage_key,
                   requested_by, requested_at, expires_at
              FROM upload_intent WHERE id = :id""";

    private final JdbcClient jdbc;

    UploadIntentAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveIntent(UploadIntent intent) {
        UploadIntent.DeclaredFile file = intent.file();
        jdbc.sql(INSERT)
                .param("id", intent.id())
                .param("shipmentId", intent.shipmentId())
                .param("siteId", intent.siteId())
                .param("type", file.type().name())
                .param("mimeType", file.mimeType())
                .param("sizeBytes", file.sizeBytes())
                .param("source", file.source().name())
                .param("capturedAt", file.capturedAt() == null ? null : Timestamp.from(file.capturedAt()))
                .param("storageKey", intent.storageKey())
                .param("actor", intent.requestedBy())
                .param("at", Timestamp.from(intent.requestedAt()))
                .param("expiresAt", Timestamp.from(intent.expiresAt()))
                .update();
    }

    @Override
    public Optional<UploadIntent> findIntent(UUID evidenceId) {
        return jdbc.sql(SELECT)
                .param("id", evidenceId)
                .query(UploadIntentAdapter::map)
                .optional();
    }

    static UploadIntent map(ResultSet rs, int rowNumber) throws SQLException {
        Timestamp capturedAt = rs.getTimestamp("captured_at");
        UploadIntent.DeclaredFile file = new UploadIntent.DeclaredFile(
                EvidenceType.valueOf(rs.getString("evidence_type")),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                EvidenceSource.valueOf(rs.getString("source")),
                capturedAt == null ? null : capturedAt.toInstant());
        return new UploadIntent(
                required(rs, "id"),
                required(rs, "shipment_id"),
                required(rs, "site_id"),
                file,
                rs.getString("storage_key"),
                required(rs, "requested_by"),
                instant(rs, "requested_at"),
                instant(rs, "expires_at"));
    }

    private static UUID required(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getObject(column, UUID.class), column);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return Objects.requireNonNull(rs.getTimestamp(column), column).toInstant();
    }
}
