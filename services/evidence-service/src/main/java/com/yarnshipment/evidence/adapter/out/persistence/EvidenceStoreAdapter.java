package com.yarnshipment.evidence.adapter.out.persistence;

import com.yarnshipment.events.evidence.EvidenceReuseDetected;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceSource;
import com.yarnshipment.evidence.domain.model.EvidenceStatus;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link EvidenceStorePort}'un JDBC uygulaması: kanıt kaydı, tekrar kullanım kayıtları ve outbox olayları aynı
 * transaction'da yazılır (EVT-03). Aynı hash başka bir sevkiyatta daha önce görüldüyse, o sevkiyattaki ilk kopya
 * için tekrar kaydı da yazılır.
 */
@Component
final class EvidenceStoreAdapter implements EvidenceStorePort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String SELECT = """
            SELECT id, shipment_id, evidence_type, storage_key, mime_type, size_bytes, sha256, source, captured_at,
                   uploaded_at, uploaded_by, status, invalidated_reason
              FROM evidence_file WHERE id = :id""";
    private static final String INSERT = """
            INSERT INTO evidence_file (id, created_at, shipment_id, evidence_type, storage_key, mime_type, size_bytes,
                                       sha256, source, captured_at, uploaded_at, uploaded_by, status, created_by)
            VALUES (:id, :at, :shipmentId, :type, :storageKey, :mimeType, :sizeBytes, :sha256, :source, :capturedAt,
                    :at, :actor, 'VALID', :actor)""";
    private static final String SELECT_BY_SHIPMENT = """
            SELECT id, shipment_id, evidence_type, storage_key, mime_type, size_bytes, sha256, source, captured_at,
                   uploaded_at, uploaded_by, status, invalidated_reason
              FROM evidence_file WHERE shipment_id = :shipmentId ORDER BY uploaded_at, id""";
    private static final String FIND_REUSE = """
            SELECT id, shipment_id FROM (
                SELECT DISTINCT ON (shipment_id) id, shipment_id, created_at FROM evidence_file
                 WHERE sha256 = :sha256 AND shipment_id <> :shipmentId AND status = 'VALID' AND id <> :id
                 ORDER BY shipment_id, created_at, id) first_per_shipment
             ORDER BY created_at, id""";
    private static final String INSERT_DUPLICATE = """
            INSERT INTO evidence_duplicate (id, evidence_id, duplicate_of_id, detected_at, created_at, created_by)
            VALUES (:id, :evidenceId, :duplicateOfId, :at, :at, :actor)""";

    private final JdbcClient jdbc;
    private final EvidenceOutbox outbox;
    private final UuidV7Generator ids;

    EvidenceStoreAdapter(JdbcClient jdbc, EvidenceOutbox outbox, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.ids = ids;
    }

    @Override
    public Optional<EvidenceFile> findEvidence(UUID evidenceId) {
        return jdbc.sql(SELECT)
                .param("id", evidenceId)
                .query(EvidenceStoreAdapter::map)
                .optional();
    }

    @Override
    public List<EvidenceFile> findByShipment(UUID shipmentId) {
        return jdbc.sql(SELECT_BY_SHIPMENT)
                .param("shipmentId", shipmentId)
                .query(EvidenceStoreAdapter::map)
                .list();
    }

    @Override
    public void saveEvidence(EvidenceFile evidence) {
        Timestamp at = Timestamp.from(evidence.uploadedAt());
        jdbc.sql(SET_ACTOR)
                .param("actor", evidence.uploadedBy().toString())
                .query()
                .singleValue();
        insert(evidence, at);
        publish(
                evidence,
                EvidenceUploaded.TYPE,
                new EvidenceUploaded(
                        evidence.id(),
                        evidence.shipmentId(),
                        evidence.declared().type().name(),
                        evidence.stored().sha256(),
                        evidence.uploadedAt()));
        for (Reuse reuse : findReuse(evidence)) {
            recordReuse(evidence, reuse, at);
        }
    }

    private void recordReuse(EvidenceFile evidence, Reuse reuse, Timestamp at) {
        jdbc.sql(INSERT_DUPLICATE)
                .param("id", ids.next())
                .param("evidenceId", evidence.id())
                .param("duplicateOfId", reuse.evidenceId())
                .param("at", at)
                .param("actor", evidence.uploadedBy())
                .update();
        publish(
                evidence,
                EvidenceReuseDetected.TYPE,
                new EvidenceReuseDetected(
                        evidence.id(),
                        evidence.shipmentId(),
                        reuse.evidenceId(),
                        reuse.shipmentId(),
                        evidence.uploadedAt()));
    }

    private void insert(EvidenceFile evidence, Timestamp at) {
        UploadIntent.DeclaredFile declared = evidence.declared();
        jdbc.sql(INSERT)
                .param("id", evidence.id())
                .param("at", at)
                .param("shipmentId", evidence.shipmentId())
                .param("type", declared.type().name())
                .param("storageKey", evidence.storageKey())
                .param("mimeType", evidence.stored().mimeType())
                .param("sizeBytes", evidence.stored().sizeBytes())
                .param("sha256", evidence.stored().sha256())
                .param("source", declared.source().name())
                .param("capturedAt", declared.capturedAt() == null ? null : Timestamp.from(declared.capturedAt()))
                .param("actor", evidence.uploadedBy())
                .update();
    }

    private List<Reuse> findReuse(EvidenceFile evidence) {
        return jdbc.sql(FIND_REUSE)
                .param("sha256", evidence.stored().sha256())
                .param("shipmentId", evidence.shipmentId())
                .param("id", evidence.id())
                .query((rs, row) -> new Reuse(
                        Objects.requireNonNull(rs.getObject("id", UUID.class)),
                        Objects.requireNonNull(rs.getObject("shipment_id", UUID.class))))
                .list();
    }

    private void publish(EvidenceFile evidence, String type, Object event) {
        outbox.publish(
                new EvidenceOutbox.Origin(evidence.shipmentId(), evidence.uploadedBy(), evidence.uploadedAt()),
                type,
                event);
    }

    static EvidenceFile map(ResultSet rs, int rowNumber) throws SQLException {
        Timestamp capturedAt = rs.getTimestamp("captured_at");
        UploadIntent.DeclaredFile declared = new UploadIntent.DeclaredFile(
                EvidenceType.valueOf(rs.getString("evidence_type")),
                rs.getString("mime_type"),
                rs.getLong("size_bytes"),
                EvidenceSource.valueOf(rs.getString("source")),
                capturedAt == null ? null : capturedAt.toInstant());
        StoredObject stored =
                new StoredObject(rs.getLong("size_bytes"), rs.getString("sha256"), rs.getString("mime_type"));
        Instant uploadedAt =
                Objects.requireNonNull(rs.getTimestamp("uploaded_at")).toInstant();
        return new EvidenceFile(
                Objects.requireNonNull(rs.getObject("id", UUID.class)),
                Objects.requireNonNull(rs.getObject("shipment_id", UUID.class)),
                rs.getString("storage_key"),
                stored,
                declared,
                uploadedAt,
                Objects.requireNonNull(rs.getObject("uploaded_by", UUID.class)),
                EvidenceStatus.valueOf(rs.getString("status")),
                rs.getString("invalidated_reason"));
    }

    /** Aynı hash'in başka sevkiyatta daha önce yüklenmiş kaydı. */
    private record Reuse(UUID evidenceId, UUID shipmentId) {}
}
