package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Sevkiyat dökümü ve fasoncu bakiyesi; ikisi de sevkiyatın oluşturulma günündeki döneme bakar. */
@Component
final class ShipmentListReader {

    private final JdbcClient jdbc;

    ShipmentListReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<ShipmentRow> findShipments(ReportPeriod period, ReportScope scope, int limit) {
        String sql = """
                SELECT shipment_id, shipment_no, subcontractor_id, status, dispatch_net_kg,
                       delivery_net_kg, had_discrepancy, created_at, completed_at
                  FROM shipment_view
                 WHERE true""" + ScopedSql.PERIOD_FILTER + ScopedSql.SITE_FILTER + """

                 ORDER BY created_at DESC, shipment_no
                 LIMIT :limit""";
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .param("limit", limit)
                .query((rs, row) -> mapShipment(rs))
                .list();
    }

    private static ShipmentRow mapShipment(ResultSet rs) throws SQLException {
        return new ShipmentRow(
                rs.getObject("shipment_id", UUID.class),
                rs.getString("shipment_no"),
                rs.getObject("subcontractor_id", UUID.class),
                rs.getString("status"),
                rs.getBigDecimal("dispatch_net_kg"),
                rs.getBigDecimal("delivery_net_kg"),
                rs.getBoolean("had_discrepancy"),
                rs.getTimestamp("created_at").toInstant(),
                instantOrNull(rs.getTimestamp("completed_at")));
    }

    List<SubcontractorBalance> findSubcontractorBalances(ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT subcontractor_id, count(*) AS shipments,
                       coalesce(sum(dispatch_net_kg), 0) AS dispatched,
                       coalesce(sum(delivery_net_kg), 0) AS delivered,
                       coalesce(sum(dispatch_net_kg) FILTER (WHERE delivery_net_kg IS NULL), 0) AS in_transit
                  FROM shipment_view
                 WHERE dispatched_at IS NOT NULL
                   AND status <> 'CANCELLED'""" + ScopedSql.PERIOD_FILTER + ScopedSql.SITE_FILTER + """

                 GROUP BY subcontractor_id
                 ORDER BY in_transit DESC, subcontractor_id""";
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .query((rs, row) -> new SubcontractorBalance(
                        rs.getObject("subcontractor_id", UUID.class),
                        rs.getInt("shipments"),
                        rs.getBigDecimal("dispatched"),
                        rs.getBigDecimal("delivered"),
                        rs.getBigDecimal("in_transit")))
                .list();
    }

    private static @Nullable Instant instantOrNull(@Nullable Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
