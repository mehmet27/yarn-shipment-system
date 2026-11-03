package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.ShipmentEvidencePort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@code shipment_evidence} tablosu; olay tekrar gelirse ilk kayıt kalır. */
@Component
final class ShipmentEvidenceAdapter implements ShipmentEvidencePort {

    /** Olaydan yazılan kayıtların sahibi; shipment-service'teki sistem hesabıyla aynı. */
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private static final String INSERT = """
            INSERT INTO shipment_evidence (evidence_id, shipment_id, evidence_type, uploaded_at, created_at, created_by)
            VALUES (:evidenceId, :shipmentId, :type, :uploadedAt, :uploadedAt, :actor)
            ON CONFLICT (evidence_id) DO NOTHING""";
    private static final String FIND_TYPES =
            "SELECT DISTINCT evidence_type FROM shipment_evidence WHERE shipment_id = :shipmentId";

    private final JdbcClient jdbc;

    ShipmentEvidenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordUploaded(UUID evidenceId, UUID shipmentId, String evidenceType, Instant uploadedAt) {
        jdbc.sql(INSERT)
                .param("evidenceId", evidenceId)
                .param("shipmentId", shipmentId)
                .param("type", evidenceType)
                .param("uploadedAt", Timestamp.from(uploadedAt))
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    @Override
    public Set<String> findEvidenceTypes(UUID shipmentId) {
        return new TreeSet<>(jdbc.sql(FIND_TYPES)
                .param("shipmentId", shipmentId)
                .query(String.class)
                .list());
    }
}
