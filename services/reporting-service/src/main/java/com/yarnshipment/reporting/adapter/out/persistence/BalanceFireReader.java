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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Fasoncu × iplik cinsi bakiyesi ve o fasoncuya o iplik için son girilen fiyat. */
    private static final String PRICED_BALANCE_SQL = """
            SELECT b.subcontractor_id, b.subcontractor_name, b.balance_kg, p.unit_price
              FROM (SELECT m.subcontractor_id, m.yarn_type,
                           max(coalesce(sub.title, s.subcontractor_title, '')) AS subcontractor_name,
                           sum(m.net_kg) AS balance_kg
                      FROM shipment.balance_movement m
                      LEFT JOIN shipment.shipment s ON s.id = m.shipment_id
                      LEFT JOIN shipment.md_subcontractor sub ON sub.id = m.subcontractor_id
                     WHERE true /*SITE*/
                     GROUP BY m.subcontractor_id, m.yarn_type) b
              LEFT JOIN LATERAL (
                   SELECT l.unit_price
                     FROM shipment.shipment_line l
                     JOIN shipment.shipment sh ON sh.id = l.shipment_id
                    WHERE sh.subcontractor_id = b.subcontractor_id
                      AND coalesce(l.yarn_type_id::text, l.yarn_type_text) = b.yarn_type
                      AND l.unit_price IS NOT NULL
                    ORDER BY sh.created_at DESC, l.line_no
                    LIMIT 1) p ON true
             WHERE b.balance_kg > 0
             ORDER BY b.subcontractor_name, b.subcontractor_id""";

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
                       fabric_kg, fire_rate, status, invoice_no, invoiced_at, claim_kg
                  FROM fire_file_view
                 WHERE (CAST(:subcontractor AS uuid) IS NULL OR subcontractor_id = CAST(:subcontractor AS uuid))
                """ + FIRE_PERIOD_FILTER + ScopedSql.SITE_FILTER + " ORDER BY completed_at DESC, shipment_no";
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .param("subcontractor", subcontractorId == null ? null : subcontractorId.toString())
                .query((rs, row) -> fireFile(rs))
                .list();
    }

    @Override
    public ShippedAmount findShippedAmount(ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT coalesce(sum(l.net_kg * l.unit_price) FILTER (WHERE l.unit_price IS NOT NULL), 0) AS amount,
                       coalesce(sum(l.net_kg) FILTER (WHERE l.unit_price IS NULL), 0) AS unpriced_kg
                  FROM shipment.shipment_line l
                  JOIN shipment_view s ON s.shipment_id = l.shipment_id
                 WHERE s.dispatched_at >= :start AND s.dispatched_at < :end"""
                + ScopedSql.SITE_FILTER.replace("site_id", "s.site_id");
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .query((rs, row) -> new ShippedAmount(rs.getBigDecimal("amount"), rs.getBigDecimal("unpriced_kg")))
                .single();
    }

    @Override
    public Map<UUID, BigDecimal> findAveragePrices(Collection<UUID> shipmentIds) {
        if (shipmentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> prices = new HashMap<>();
        jdbc.sql("""
                        SELECT shipment_id,
                               sum(net_kg * unit_price) / sum(net_kg) AS average_price
                          FROM shipment.shipment_line
                         WHERE shipment_id IN (:ids) AND unit_price IS NOT NULL
                         GROUP BY shipment_id""")
                .param("ids", List.copyOf(shipmentIds))
                .query((rs, row) -> {
                    prices.put(rs.getObject("shipment_id", UUID.class), rs.getBigDecimal("average_price"));
                    return null;
                })
                .list();
        return prices;
    }

    @Override
    public List<PricedBalance> findPricedBalances(ReportScope scope) {
        String sql = PRICED_BALANCE_SQL.replace("/*SITE*/", MOVEMENT_SITE_FILTER.replace("site_id", "s.site_id"));
        return ScopedSql.bind(jdbc.sql(sql), scope)
                .query((rs, row) -> new PricedBalance(
                        rs.getObject("subcontractor_id", UUID.class),
                        rs.getString("subcontractor_name"),
                        rs.getBigDecimal("balance_kg"),
                        rs.getBigDecimal("unit_price")))
                .list();
    }

    /**
     * Açılış, defterin kendisine yazılır ({@code balance_movement_view} bir görünümdür). {@code source_ref}
     * (fasoncu, iplik) başına tek açılış sağlar; defter sıfır kiloyu kabul etmez.
     */
    @Override
    public boolean insertOpeningIfAbsent(OpeningBalanceRow row, Instant occurredAt, UUID actorId) {
        if (row.netKg().signum() == 0) {
            return false;
        }
        int rows = jdbc.sql("""
                        INSERT INTO shipment.balance_movement (id, subcontractor_id, yarn_type, kind, net_kg,
                                                               occurred_at, created_by, source_ref)
                        VALUES (gen_random_uuid(), :sub, :yarn, 'OPENING', :kg, :at, :actor,
                                'OPENING:' || :sub || ':' || :yarn)
                        ON CONFLICT DO NOTHING""")
                .param("sub", row.subcontractorId())
                .param("yarn", row.yarnType())
                .param("kg", row.netKg())
                .param("at", Timestamp.from(occurredAt))
                .param("actor", actorId)
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
                instant(rs.getTimestamp("invoiced_at")),
                rs.getBigDecimal("claim_kg"));
    }
}
