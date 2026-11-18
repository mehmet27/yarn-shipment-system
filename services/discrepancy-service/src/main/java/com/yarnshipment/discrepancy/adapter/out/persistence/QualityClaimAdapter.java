package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link QualityClaimPort}'un JDBC uygulaması. Hedef sevkiyat izleme kaydından (shipment_watch), itiraz fotoğrafları
 * uyuşmazlık geçmişindeki {@code CLAIM_EVIDENCE} kayıtlarından okunur.
 */
@Component
final class QualityClaimAdapter implements QualityClaimPort {

    static final String CLAIM_EVIDENCE = "CLAIM_EVIDENCE";

    private static final String TARGET = """
            SELECT w.shipment_id, w.site_id, w.subcontractor_id, w.shipment_no, w.completed_at,
                   (SELECT COALESCE(SUM(f.net_kg), 0) FROM fabric_receipt_watch f
                     WHERE f.shipment_id = w.shipment_id) AS received_kg,
                   (SELECT COALESCE(SUM(q.claimed_kg), 0) FROM quality_claim q
                      JOIN discrepancy d ON d.id = q.discrepancy_id
                     WHERE q.shipment_id = w.shipment_id
                       AND d.resolution IS DISTINCT FROM 'CLAIM_UNJUSTIFIED') AS claimed_kg
              FROM shipment_watch w
             WHERE w.shipment_id = :shipmentId AND w.completed_at IS NOT NULL""";
    private static final String CLAIM = """
            SELECT id, discrepancy_id, shipment_id, subcontractor_id, opened_by, opened_at, deadline_at, description,
                   claimed_kg, fabric_receipt_id
              FROM quality_claim""";
    private static final String EVIDENCE_IDS = """
            SELECT evidence_id FROM discrepancy_activity
             WHERE discrepancy_id = :discrepancyId AND action_type = :action AND evidence_id IS NOT NULL
             ORDER BY occurred_at, id""";

    private static final int KG_SCALE = 3;

    private final JdbcClient jdbc;
    private final QualityClaimWriter writer;

    QualityClaimAdapter(JdbcClient jdbc, QualityClaimWriter writer) {
        this.jdbc = jdbc;
        this.writer = writer;
    }

    @Override
    public Optional<ClaimTarget> findTarget(UUID shipmentId) {
        return jdbc.sql(TARGET)
                .param("shipmentId", shipmentId)
                .query((rs, row) -> new ClaimTarget(
                        ClaimRows.requiredUuid(rs, "shipment_id"),
                        ClaimRows.requiredUuid(rs, "site_id"),
                        ClaimRows.optionalUuid(rs, "subcontractor_id"),
                        rs.getString("shipment_no"),
                        ClaimRows.requiredInstant(rs, "completed_at"),
                        Objects.requireNonNull(rs.getBigDecimal("received_kg"), "received_kg"),
                        Objects.requireNonNull(rs.getBigDecimal("claimed_kg"), "claimed_kg")))
                .optional();
    }

    @Override
    public List<QualityClaim> findClaims(UUID shipmentId) {
        return jdbc
                .sql(CLAIM + " WHERE shipment_id = :shipmentId ORDER BY opened_at, id")
                .param("shipmentId", shipmentId)
                .query(QualityClaimAdapter::mapHeader)
                .list()
                .stream()
                .map(this::withEvidence)
                .toList();
    }

    @Override
    public Optional<QualityClaim> findClaim(UUID claimId) {
        return jdbc.sql(CLAIM + " WHERE id = :id")
                .param("id", claimId)
                .query(QualityClaimAdapter::mapHeader)
                .optional()
                .map(this::withEvidence);
    }

    @Override
    public QualityClaim saveClaim(NewQualityClaim claim) {
        return writer.write(claim);
    }

    /** Fotoğraflar ayrı sorguyla, ana sonuç kümesi kapandıktan sonra okunur. */
    private QualityClaim withEvidence(QualityClaim header) {
        return new QualityClaim(
                header.id(),
                header.discrepancyId(),
                header.shipmentId(),
                header.subcontractorId(),
                header.opening(),
                header.description(),
                header.claimedKg(),
                header.fabricReceiptId(),
                nonNullIds(jdbc.sql(EVIDENCE_IDS)
                        .param("discrepancyId", header.discrepancyId())
                        .param("action", CLAIM_EVIDENCE)
                        .query(UUID.class)
                        .list()));
    }

    static QualityClaim mapHeader(ResultSet rs, int rowNumber) throws SQLException {
        return new QualityClaim(
                ClaimRows.requiredUuid(rs, "id"),
                ClaimRows.requiredUuid(rs, "discrepancy_id"),
                ClaimRows.requiredUuid(rs, "shipment_id"),
                ClaimRows.optionalUuid(rs, "subcontractor_id"),
                new QualityClaim.Opening(
                        ClaimRows.requiredUuid(rs, "opened_by"),
                        ClaimRows.requiredInstant(rs, "opened_at"),
                        ClaimRows.requiredInstant(rs, "deadline_at")),
                rs.getString("description"),
                legacyZero(rs.getBigDecimal("claimed_kg")),
                ClaimRows.optionalUuid(rs, "fabric_receipt_id"),
                List.of());
    }

    /** Çuval bazlı eski itirazlarda kilo yoktur; sıfır gösterilir. */
    private static BigDecimal legacyZero(@Nullable BigDecimal claimedKg) {
        return claimedKg == null ? BigDecimal.ZERO.setScale(KG_SCALE) : claimedKg;
    }

    /** Sorgulanan kolonlar {@code IS NOT NULL} ile süzülü olduğu için boş değer gelmez. */
    private static List<UUID> nonNullIds(List<@Nullable UUID> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull)
                .toList();
    }
}
