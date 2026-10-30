package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.discrepancy.domain.model.Severity;
import com.yarnshipment.events.discrepancy.DiscrepancyOpened;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kalite itirazını yazar: Kırmızı {@code QUALITY} uyuşmazlığı, itiraz ve çuvalları, açılış ve fotoğraf geçmişi ve
 * {@code DiscrepancyOpened} olayı aynı transaction'da (EVT-03, PERS-20). Uyuşmazlık numarası sevkiyatın sıradaki
 * numarasıdır; aynı sevkiyata eşzamanlı iki itiraz aynı numarayı almasın diye sevkiyat bazında kilit alınır.
 */
@Component
final class QualityClaimWriter {

    static final String RULE_CODE = "QUALITY_CLAIM";

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String LOCK_SHIPMENT = "SELECT pg_advisory_xact_lock(hashtext(:shipmentId))";
    private static final String COUNT_DISCREPANCIES =
            "SELECT count(*) FROM discrepancy WHERE shipment_id = :shipmentId";
    private static final String INSERT_DISCREPANCY = """
            INSERT INTO discrepancy (id, discrepancy_no, site_id, shipment_id, subcontractor_id, discrepancy_type,
                                     severity, status, rule_code, actual_value, due_at, created_at, created_by)
            VALUES (:id, :no, :siteId, :shipmentId, :subcontractorId, :type, :severity, 'OPEN', :ruleCode, :actual,
                    :dueAt, :at, :actor)""";
    private static final String INSERT_CLAIM = """
            INSERT INTO quality_claim (id, discrepancy_id, shipment_id, subcontractor_id, opened_by, opened_at,
                                       deadline_at, description, created_at, created_by)
            VALUES (:id, :discrepancyId, :shipmentId, :subcontractorId, :actor, :at, :deadlineAt, :description, :at,
                    :actor)""";
    private static final String INSERT_BAG = """
            INSERT INTO quality_claim_bag (claim_id, unique_key, created_at, created_by)
            VALUES (:claimId, :uniqueKey, :at, :actor)""";
    private static final String INSERT_ACTIVITY = """
            INSERT INTO discrepancy_activity (id, created_at, discrepancy_id, action_type, note, evidence_id, actor_id,
                                              occurred_at)
            VALUES (:id, :at, :discrepancyId, :action, :note, :evidenceId, :actor, :at)""";
    private static final String INSERT_OUTBOX = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (:id, :at, :aggregateType, :shipmentId, :type, :payload::jsonb, :headers::jsonb, :at)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    QualityClaimWriter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    QualityClaim write(NewQualityClaim claim) {
        ClaimTarget target = claim.target();
        jdbc.sql(SET_ACTOR).param("actor", claim.openedBy().toString()).query().singleValue();
        jdbc.sql(LOCK_SHIPMENT)
                .param("shipmentId", target.shipmentId().toString())
                .query()
                .singleValue();
        QualityClaim written = new QualityClaim(
                ids.next(),
                ids.next(),
                target.shipmentId(),
                target.subcontractorId(),
                new QualityClaim.Opening(claim.openedBy(), claim.openedAt(), target.deadline()),
                claim.request().description().strip(),
                claim.request().bagKeys(),
                claim.request().evidenceIds());
        String discrepancyNo = nextDiscrepancyNo(target);
        insertDiscrepancy(claim, written, discrepancyNo);
        insertClaim(written);
        insertActivities(written);
        publishOpened(claim, written, discrepancyNo);
        return written;
    }

    private String nextDiscrepancyNo(ClaimTarget target) {
        long existing = jdbc.sql(COUNT_DISCREPANCIES)
                .param("shipmentId", target.shipmentId())
                .query(Long.class)
                .single();
        String shipment = target.shipmentNo() == null ? target.shipmentId().toString() : target.shipmentNo();
        return "UY-" + shipment + "-" + (existing + 1);
    }

    private void insertDiscrepancy(NewQualityClaim claim, QualityClaim written, String discrepancyNo) {
        jdbc.sql(INSERT_DISCREPANCY)
                .param("id", written.discrepancyId())
                .param("no", discrepancyNo)
                .param("siteId", claim.target().siteId())
                .param("shipmentId", written.shipmentId())
                .param("subcontractorId", written.subcontractorId())
                .param("type", DiscrepancyType.QUALITY.name())
                .param("severity", Severity.RED.name())
                .param("ruleCode", RULE_CODE)
                .param("actual", String.join(", ", written.bagKeys()))
                .param("dueAt", Timestamp.from(claim.dueAt()))
                .param("at", Timestamp.from(written.opening().openedAt()))
                .param("actor", written.opening().openedBy())
                .update();
    }

    private void insertClaim(QualityClaim written) {
        Timestamp at = Timestamp.from(written.opening().openedAt());
        jdbc.sql(INSERT_CLAIM)
                .param("id", written.id())
                .param("discrepancyId", written.discrepancyId())
                .param("shipmentId", written.shipmentId())
                .param("subcontractorId", written.subcontractorId())
                .param("actor", written.opening().openedBy())
                .param("at", at)
                .param("deadlineAt", Timestamp.from(written.opening().deadlineAt()))
                .param("description", written.description())
                .update();
        for (String key : written.bagKeys()) {
            jdbc.sql(INSERT_BAG)
                    .param("claimId", written.id())
                    .param("uniqueKey", key)
                    .param("at", at)
                    .param("actor", written.opening().openedBy())
                    .update();
        }
    }

    private void insertActivities(QualityClaim written) {
        insertActivity(written, "OPENED", written.description(), null);
        for (UUID evidenceId : written.evidenceIds()) {
            insertActivity(written, QualityClaimAdapter.CLAIM_EVIDENCE, null, evidenceId);
        }
    }

    private void insertActivity(QualityClaim written, String action, @Nullable String note, @Nullable UUID evidenceId) {
        jdbc.sql(INSERT_ACTIVITY)
                .param("id", ids.next())
                .param("at", Timestamp.from(written.opening().openedAt()))
                .param("discrepancyId", written.discrepancyId())
                .param("action", action)
                .param("note", note)
                .param("evidenceId", evidenceId)
                .param("actor", written.opening().openedBy())
                .update();
    }

    private void publishOpened(NewQualityClaim claim, QualityClaim written, String discrepancyNo) {
        DiscrepancyOpened opened = new DiscrepancyOpened(
                written.discrepancyId(),
                discrepancyNo,
                written.shipmentId(),
                claim.target().siteId(),
                DiscrepancyType.QUALITY.name(),
                Severity.RED.name(),
                RULE_CODE,
                claim.dueAt());
        jdbc.sql(INSERT_OUTBOX)
                .param("id", ids.next())
                .param("at", Timestamp.from(written.opening().openedAt()))
                .param("aggregateType", AGGREGATE_TYPE)
                .param("shipmentId", written.shipmentId())
                .param("type", DiscrepancyOpened.TYPE)
                .param("payload", jsonMapper.writeValueAsString(opened))
                .param(
                        "headers",
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", claim.openedBy().toString())))
                .update();
    }
}
