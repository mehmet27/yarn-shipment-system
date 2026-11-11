package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Fasoncu bazlı göstergeler. En yüksek uyuşmazlık oranı önce gelir. */
@Component
final class PartyReader {

    private final JdbcClient jdbc;

    PartyReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<PartyStats> findSubcontractorStats(ReportPeriod period, ReportScope scope) {
        return findPartyStats("subcontractor_id", period, scope);
    }

    /** {@code partyColumn} koddan gelen sabit bir kolon adıdır, kullanıcı girdisi değildir. */
    private List<PartyStats> findPartyStats(String partyColumn, ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT %1$s AS party_id, count(*) AS shipments,
                       count(*) FILTER (WHERE had_discrepancy) AS discrepant
                  FROM shipment_view
                 WHERE dispatched_at IS NOT NULL AND status <> 'CANCELLED' AND %1$s IS NOT NULL%2$s
                 GROUP BY %1$s
                 ORDER BY count(*) FILTER (WHERE had_discrepancy)::numeric / count(*) DESC, count(*) DESC,
                          %1$s""".formatted(partyColumn, ScopedSql.PERIOD_FILTER + ScopedSql.SITE_FILTER);
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .query((rs, row) -> new PartyStats(
                        rs.getObject("party_id", UUID.class),
                        rs.getInt("shipments"),
                        rs.getInt("discrepant")))
                .list();
    }
}
