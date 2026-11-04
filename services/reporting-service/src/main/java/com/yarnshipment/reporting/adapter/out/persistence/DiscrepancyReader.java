package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.OpenDiscrepancies;
import com.yarnshipment.reporting.domain.model.OverdueDiscrepancy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Açık ve vadesi geçmiş uyuşmazlık göstergeleri; dönemden bağımsızdır. */
@Component
final class DiscrepancyReader {

    private static final int OVERDUE_LIMIT = 10;
    private static final String OPEN_BASE = " FROM discrepancy_view WHERE resolved_at IS NULL" + ScopedSql.SITE_FILTER;

    private final JdbcClient jdbc;

    DiscrepancyReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    OpenDiscrepancies findOpen(ReportScope scope, Instant now) {
        String sql = """
                SELECT count(*) AS open,
                       count(*) FILTER (WHERE severity = 'RED') AS red,
                       count(*) FILTER (WHERE severity = 'YELLOW') AS yellow,
                       count(*) FILTER (WHERE due_at < :now) AS overdue""" + OPEN_BASE;
        return ScopedSql.bind(jdbc.sql(sql), scope)
                .param("now", Timestamp.from(now))
                .query((rs, row) -> new OpenDiscrepancies(
                        rs.getInt("open"), rs.getInt("red"), rs.getInt("yellow"), rs.getInt("overdue")))
                .single();
    }

    List<OverdueDiscrepancy> findOverdue(ReportScope scope, Instant now) {
        String sql = "SELECT discrepancy_id, discrepancy_no, shipment_id, severity, due_at"
                + OPEN_BASE
                + " AND due_at < :now ORDER BY due_at, discrepancy_no LIMIT "
                + OVERDUE_LIMIT;
        return ScopedSql.bind(jdbc.sql(sql), scope)
                .param("now", Timestamp.from(now))
                .query((rs, row) -> new OverdueDiscrepancy(
                        rs.getObject("discrepancy_id", UUID.class),
                        rs.getString("discrepancy_no"),
                        rs.getObject("shipment_id", UUID.class),
                        rs.getString("severity"),
                        rs.getTimestamp("due_at").toInstant()))
                .list();
    }
}
