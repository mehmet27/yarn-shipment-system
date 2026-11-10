package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Bakiye matrisi ve fire dosyası sorguları ile açılış bakiyesi yazımı. */
@Component
final class BalanceFireReader implements BalanceFirePort {

    /** Lokasyonu olmayan hareketler (OPENING) kapsamdaki herkese görünür. */
    private static final String MOVEMENT_SITE_FILTER = " AND (site_id IS NULL OR ((:allSites OR site_id IN (:siteIds))"
            + " AND (CAST(:requestedSite AS uuid) IS NULL OR site_id = CAST(:requestedSite AS uuid))))";

    private static final String FIRE_PERIOD_FILTER = " AND completed_at >= :start AND completed_at < :end";

    private final JdbcClient jdbc;

    BalanceFireReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<BalanceMatrixRow> findBalanceMatrix(ReportScope scope) {
        String sql = """
                SELECT m.yarn_type, m.subcontractor_id, max(m.subcontractor_name) AS subcontractor_name,
                       sum(m.net_kg) AS balance,
                       coalesce(sum(m.net_kg) FILTER (WHERE m.kind = 'DISPATCH' AND s.status = 'AWAITING_DELIVERY'), 0)
                           AS in_transit
                  FROM balance_movement_view m
                  LEFT JOIN shipment_view s ON s.shipment_id = m.shipment_id
                 WHERE true""" + MOVEMENT_SITE_FILTER.replace("site_id", "m.site_id") + """

                 GROUP BY m.yarn_type, m.subcontractor_id
                 ORDER BY m.yarn_type, max(m.subcontractor_name), m.subcontractor_id""";
        return ScopedSql.bind(jdbc.sql(sql), scope)
                .query((rs, row) -> new BalanceMatrixRow(
                        rs.getString("yarn_type"),
                        rs.getObject("subcontractor_id", UUID.class),
                        rs.getString("subcontractor_name"),
                        rs.getBigDecimal("balance"),
                        rs.getBigDecimal("in_transit")))
                .list();
    }

    @Override
    public List<FireFile> findFireFiles(ReportPeriod period, ReportScope scope, @Nullable UUID subcontractorId) {
        String sql = """
                SELECT shipment_id, shipment_no, subcontractor_id, subcontractor_name, sent_kg, returned_yarn_kg,
                       fabric_kg, fire_rate, status, invoice_no, invoiced_at
                  FROM fire_file_view
                 WHERE (CAST(:subcontractor AS uuid) IS NULL OR subcontractor_id = CAST(:subcontractor AS uuid))
                """ + FIRE_PERIOD_FILTER + ScopedSql.SITE_FILTER + " ORDER BY completed_at DESC, shipment_no";
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .param("subcontractor", subcontractorId == null ? null : subcontractorId.toString())
                .query((rs, row) -> fireFile(rs))
                .list();
    }

    @Override
    public boolean insertOpeningIfAbsent(OpeningBalanceRow row, Instant occurredAt) {
        int rows = jdbc.sql("""
                        INSERT INTO balance_movement_view (id, subcontractor_id, subcontractor_name, yarn_type, kind,
                                                           net_kg, occurred_at)
                        VALUES (gen_random_uuid(), :sub, :name, :yarn, 'OPENING', :kg, :at)
                        ON CONFLICT DO NOTHING""")
                .param("sub", row.subcontractorId())
                .param("name", row.subcontractorName())
                .param("yarn", row.yarnType())
                .param("kg", row.netKg())
                .param("at", Timestamp.from(occurredAt))
                .update();
        return rows > 0;
    }

    private static @Nullable Instant instant(@Nullable Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static FireFile fireFile(ResultSet rs) throws SQLException {
        return new FireFile(
                rs.getObject("shipment_id", UUID.class),
                rs.getString("shipment_no"),
                rs.getObject("subcontractor_id", UUID.class),
                rs.getString("subcontractor_name"),
                rs.getBigDecimal("sent_kg"),
                rs.getBigDecimal("returned_yarn_kg"),
                rs.getBigDecimal("fabric_kg"),
                rs.getBigDecimal("fire_rate"),
                rs.getString("status"),
                rs.getString("invoice_no"),
                instant(rs.getTimestamp("invoiced_at")));
    }
}
