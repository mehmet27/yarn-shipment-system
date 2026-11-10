package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.BandCounts;
import com.yarnshipment.reporting.domain.model.LargestDifference;
import com.yarnshipment.reporting.domain.model.QuantitySummary;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentTotals;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Dashboard'un dönemlik sevkiyat göstergeleri. */
@Component
final class DashboardReader {

    private static final int LARGEST_LIMIT = 5;
    private static final String LARGEST_SELECT = """
            SELECT shipment_id, shipment_no, diff_kg, diff_pct, band
              FROM shipment_view
             WHERE band IS NOT NULL AND diff_kg IS NOT NULL""";
    private static final String LARGEST_ORDER = " ORDER BY abs(diff_kg) DESC, shipment_no LIMIT " + LARGEST_LIMIT;
    private static final String FILTERS = ScopedSql.PERIOD_FILTER + ScopedSql.SITE_FILTER;

    private final JdbcClient jdbc;

    DashboardReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    ShipmentTotals findTotals(ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE status IN ('DRAFT', 'AWAITING_DELIVERY')) AS in_progress,
                       count(*) FILTER (WHERE status = 'IN_REVIEW') AS in_review,
                       count(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                       count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled
                  FROM shipment_view WHERE true""" + FILTERS;
        return query(sql, period, scope)
                .query((rs, row) -> new ShipmentTotals(
                        rs.getInt("total"),
                        rs.getInt("in_progress"),
                        rs.getInt("in_review"),
                        rs.getInt("completed"),
                        rs.getInt("cancelled")))
                .single();
    }

    QuantitySummary findQuantity(ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT coalesce(sum(dispatch_net_kg), 0) AS dispatch_net,
                       coalesce(sum(delivery_net_kg), 0) AS delivery_net,
                       coalesce(sum(abs(diff_kg)), 0) AS abs_diff, count(*) AS compared
                  FROM shipment_view WHERE band IS NOT NULL""" + FILTERS;
        return query(sql, period, scope)
                .query((rs, row) -> new QuantitySummary(
                        rs.getBigDecimal("dispatch_net"),
                        rs.getBigDecimal("delivery_net"),
                        rs.getBigDecimal("abs_diff"),
                        rs.getInt("compared")))
                .single();
    }

    BandCounts findBands(ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT count(*) FILTER (WHERE band = 'GREEN') AS green,
                       count(*) FILTER (WHERE band = 'YELLOW') AS yellow,
                       count(*) FILTER (WHERE band = 'RED') AS red
                  FROM shipment_view WHERE band IS NOT NULL""" + FILTERS;
        return query(sql, period, scope)
                .query((rs, row) -> new BandCounts(rs.getInt("green"), rs.getInt("yellow"), rs.getInt("red")))
                .single();
    }

    List<LargestDifference> findLargest(ReportPeriod period, ReportScope scope) {
        String sql = LARGEST_SELECT + FILTERS + LARGEST_ORDER;
        return query(sql, period, scope)
                .query((rs, row) -> new LargestDifference(
                        rs.getObject("shipment_id", UUID.class),
                        rs.getString("shipment_no"),
                        rs.getBigDecimal("diff_kg"),
                        rs.getBigDecimal("diff_pct"),
                        rs.getString("band")))
                .list();
    }

    private JdbcClient.StatementSpec query(String sql, ReportPeriod period, ReportScope scope) {
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period);
    }
}
