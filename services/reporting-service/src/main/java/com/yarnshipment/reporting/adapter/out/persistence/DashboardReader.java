package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.QuantitySummary;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentTotals;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Dashboard'un dönemlik sevkiyat göstergeleri. */
@Component
final class DashboardReader {

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
                       coalesce(sum(delivery_net_kg), 0) AS delivery_net
                  FROM shipment_view WHERE status = 'COMPLETED'""" + FILTERS;
        return query(sql, period, scope)
                .query((rs, row) -> new QuantitySummary(rs.getBigDecimal("dispatch_net"), rs.getBigDecimal("delivery_net")))
                .single();
    }

    private JdbcClient.StatementSpec query(String sql, ReportPeriod period, ReportScope scope) {
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period);
    }
}
