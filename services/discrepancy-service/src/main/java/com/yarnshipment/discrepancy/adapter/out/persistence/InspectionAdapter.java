package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.InspectionPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link InspectionPort}'un JDBC uygulaması. İtiraz kontrolleri {@code reason = 'CLAIM'} ile yazılır; istek ve sonuç
 * uyuşmazlık geçmişine de düşer. Sonuç yalnızca henüz girilmemiş satıra yazılır; iki kişi aynı anda girerse ikincisi
 * reddedilir.
 */
@Component
final class InspectionAdapter implements InspectionPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String SELECT = """
            SELECT id, shipment_id, discrepancy_id, kind, request_note, created_by, selected_at, result,
                   result_evidence_id, performed_by, performed_at
              FROM inspection""";
    private static final String CLAIM_KINDS = " kind IN ('SAMPLE', 'LAB') AND discrepancy_id IS NOT NULL";
    private static final String INSERT = """
            INSERT INTO inspection (id, shipment_id, discrepancy_id, reason, kind, selected_at, request_note,
                                    created_at, created_by)
            VALUES (:id, :shipmentId, :discrepancyId, 'CLAIM', :kind, :at, :note, :at, :actor)""";
    private static final String UPDATE_RESULT = """
            UPDATE inspection
               SET performed_at = :at, performed_by = :actor, result = :result, result_evidence_id = :evidenceId,
                   updated_at = :at, updated_by = :actor, version = version + 1
             WHERE id = :id AND performed_at IS NULL""";
    private static final String INSERT_ACTIVITY = """
            INSERT INTO discrepancy_activity (id, created_at, discrepancy_id, action_type, note, evidence_id, actor_id,
                                              occurred_at)
            VALUES (:id, :at, :discrepancyId, :action, :note, :evidenceId, :actor, :at)""";

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;

    InspectionAdapter(JdbcClient jdbc, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public List<Inspection> findInspections(UUID discrepancyId) {
        return jdbc.sql(SELECT + " WHERE discrepancy_id = :discrepancyId AND" + CLAIM_KINDS
                        + " ORDER BY selected_at, id")
                .param("discrepancyId", discrepancyId)
                .query(ClaimRows::mapInspection)
                .list();
    }

    @Override
    public Optional<Inspection> findInspection(UUID inspectionId) {
        return jdbc.sql(SELECT + " WHERE id = :id AND" + CLAIM_KINDS)
                .param("id", inspectionId)
                .query(ClaimRows::mapInspection)
                .optional();
    }

    @Override
    public Inspection saveRequest(NewInspection inspection) {
        Inspection.Request request = inspection.request();
        Inspection saved = new Inspection(
                ids.next(), inspection.shipmentId(), inspection.discrepancyId(), inspection.kind(), request, null);
        setActor(request.requestedBy());
        jdbc.sql(INSERT)
                .param("id", saved.id())
                .param("shipmentId", saved.shipmentId())
                .param("discrepancyId", saved.discrepancyId())
                .param("kind", saved.kind().name())
                .param("at", Timestamp.from(request.requestedAt()))
                .param("note", request.note())
                .param("actor", request.requestedBy())
                .update();
        String note =
                request.note() == null ? saved.kind().name() : saved.kind().name() + ": " + request.note();
        insertActivity(saved, new ActivityEntry("INSPECTION_REQUESTED", note, null), request.requestedAt());
        return saved;
    }

    @Override
    public void saveResult(Inspection recorded) {
        Inspection.Result result = Objects.requireNonNull(recorded.result(), "result");
        setActor(result.recordedBy());
        int updated = jdbc.sql(UPDATE_RESULT)
                .param("id", recorded.id())
                .param("at", Timestamp.from(result.recordedAt()))
                .param("actor", result.recordedBy())
                .param("result", result.summary())
                .param("evidenceId", result.evidenceId())
                .update();
        if (updated == 0) {
            throw new ClaimRejectedException(ClaimRejection.INSPECTION_RECORDED, "inspectionId=" + recorded.id());
        }
        insertActivity(
                recorded,
                new ActivityEntry("INSPECTION_RESULT", result.summary(), result.evidenceId()),
                result.recordedAt());
    }

    private void setActor(UUID actorId) {
        jdbc.sql(SET_ACTOR).param("actor", actorId.toString()).query().singleValue();
    }

    private void insertActivity(Inspection inspection, ActivityEntry entry, Instant at) {
        UUID actor = inspection.result() == null
                ? inspection.request().requestedBy()
                : Objects.requireNonNull(inspection.result()).recordedBy();
        jdbc.sql(INSERT_ACTIVITY)
                .param("id", ids.next())
                .param("at", Timestamp.from(at))
                .param("discrepancyId", inspection.discrepancyId())
                .param("action", entry.action())
                .param("note", entry.note())
                .param("evidenceId", entry.evidenceId())
                .param("actor", actor)
                .update();
    }

    /** Uyuşmazlık geçmişine düşen kayıt. */
    private record ActivityEntry(
            String action, String note, @Nullable UUID evidenceId) {}
}
