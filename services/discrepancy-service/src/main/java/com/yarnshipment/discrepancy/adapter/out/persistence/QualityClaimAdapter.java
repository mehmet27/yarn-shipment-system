package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.application.port.out.QualityClaimPort;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
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
 * {@link QualityClaimPort}'un JDBC uygulaması. Teslim bilgisi karşılaştırma girdisinden, itiraz fotoğrafları
 * uyuşmazlık geçmişindeki {@code CLAIM_EVIDENCE} kayıtlarından okunur.
 */
@Component
final class QualityClaimAdapter implements QualityClaimPort {

    static final String CLAIM_EVIDENCE = "CLAIM_EVIDENCE";

    private static final String TARGET = """
            SELECT shipment_id, site_id, subcontractor_id, shipment_no, received_at
              FROM comparison_input
             WHERE shipment_id = :shipmentId AND site_id IS NOT NULL AND subcontractor_id IS NOT NULL""";
    private static final String DELIVERED_KEYS = """
            SELECT key.value
              FROM comparison_input, jsonb_array_elements_text(delivery_bag_keys) WITH ORDINALITY AS key(value, n)
             WHERE shipment_id = :shipmentId
             ORDER BY key.n""";
    private static final String CLAIM = """
            SELECT id, discrepancy_id, shipment_id, subcontractor_id, opened_by, opened_at, deadline_at, description
              FROM quality_claim""";
    private static final String BAG_KEYS =
            "SELECT unique_key FROM quality_claim_bag WHERE claim_id = :claimId ORDER BY unique_key";
    private static final String EVIDENCE_IDS = """
            SELECT evidence_id FROM discrepancy_activity
             WHERE discrepancy_id = :discrepancyId AND action_type = :action AND evidence_id IS NOT NULL
             ORDER BY occurred_at, id""";

    private final JdbcClient jdbc;
    private final QualityClaimWriter writer;

    QualityClaimAdapter(JdbcClient jdbc, QualityClaimWriter writer) {
        this.jdbc = jdbc;
        this.writer = writer;
    }

    @Override
    public Optional<ClaimTarget> findTarget(UUID shipmentId) {
        List<String> keys = nonNull(jdbc.sql(DELIVERED_KEYS)
                .param("shipmentId", shipmentId)
                .query(String.class)
                .list());
        return jdbc.sql(TARGET)
                .param("shipmentId", shipmentId)
                .query((rs, row) -> new ClaimTarget(
                        ClaimRows.requiredUuid(rs, "shipment_id"),
                        ClaimRows.requiredUuid(rs, "site_id"),
                        ClaimRows.requiredUuid(rs, "subcontractor_id"),
                        rs.getString("shipment_no"),
                        ClaimRows.requiredInstant(rs, "received_at"),
                        keys))
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
                .map(this::withBagsAndEvidence)
                .toList();
    }

    @Override
    public Optional<QualityClaim> findClaim(UUID claimId) {
        return jdbc.sql(CLAIM + " WHERE id = :id")
                .param("id", claimId)
                .query(QualityClaimAdapter::mapHeader)
                .optional()
                .map(this::withBagsAndEvidence);
    }

    @Override
    public QualityClaim saveClaim(NewQualityClaim claim) {
        return writer.write(claim);
    }

    /** Çuvallar ve fotoğraflar ayrı sorgularla, ana sonuç kümesi kapandıktan sonra okunur. */
    private QualityClaim withBagsAndEvidence(QualityClaim header) {
        return new QualityClaim(
                header.id(),
                header.discrepancyId(),
                header.shipmentId(),
                header.subcontractorId(),
                header.opening(),
                header.description(),
                nonNull(jdbc.sql(BAG_KEYS)
                        .param("claimId", header.id())
                        .query(String.class)
                        .list()),
                nonNullIds(jdbc.sql(EVIDENCE_IDS)
                        .param("discrepancyId", header.discrepancyId())
                        .param("action", CLAIM_EVIDENCE)
                        .query(UUID.class)
                        .list()));
    }

    private static QualityClaim mapHeader(ResultSet rs, int rowNumber) throws SQLException {
        return new QualityClaim(
                ClaimRows.requiredUuid(rs, "id"),
                ClaimRows.requiredUuid(rs, "discrepancy_id"),
                ClaimRows.requiredUuid(rs, "shipment_id"),
                ClaimRows.requiredUuid(rs, "subcontractor_id"),
                new QualityClaim.Opening(
                        ClaimRows.requiredUuid(rs, "opened_by"),
                        ClaimRows.requiredInstant(rs, "opened_at"),
                        ClaimRows.requiredInstant(rs, "deadline_at")),
                rs.getString("description"),
                List.of(),
                List.of());
    }

    /** Sorgulanan kolonlar NOT NULL ya da {@code IS NOT NULL} ile süzülü olduğu için boş değer gelmez. */
    private static List<String> nonNull(List<@Nullable String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull)
                .toList();
    }

    private static List<UUID> nonNullIds(List<@Nullable UUID> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull)
                .toList();
    }
}
