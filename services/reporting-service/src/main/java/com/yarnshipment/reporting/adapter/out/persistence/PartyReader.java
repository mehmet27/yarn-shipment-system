package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Şoför, fasoncu ve eşleşme bazlı göstergeler. En yüksek uyuşmazlık oranı önce gelir. */
@Component
final class PartyReader {

    private final JdbcClient jdbc;

    PartyReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<PartyStats> findDriverStats(ReportPeriod period, ReportScope scope) {
        return findPartyStats("driver_id", period, scope);
    }

    List<PartyStats> findSubcontractorStats(ReportPeriod period, ReportScope scope) {
        return findPartyStats("subcontractor_id", period, scope);
    }

    List<PairStats> findPairStats(ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT driver_id, subcontractor_id, sum(shipment_count) AS shipments,
                       sum(discrepant_shipment_count) AS discrepant, sum(discrepancy_count) AS discrepancies,
                       sum(total_diff_kg) AS total_diff
                  FROM pair_daily_stats
                 WHERE day BETWEEN :fromDay AND :toDay""" + ScopedSql.SITE_FILTER + """

                 GROUP BY driver_id, subcontractor_id
                 ORDER BY sum(discrepant_shipment_count)::numeric / sum(shipment_count) DESC,
                          sum(shipment_count) DESC, driver_id, subcontractor_id""";
        return ScopedSql.bind(jdbc.sql(sql), scope)
                .param("fromDay", period.from())
                .param("toDay", period.to())
                .query((rs, row) -> new PairStats(
                        rs.getObject("driver_id", UUID.class),
                        rs.getObject("subcontractor_id", UUID.class),
                        rs.getInt("shipments"),
                        rs.getInt("discrepant"),
                        rs.getInt("discrepancies"),
                        rs.getBigDecimal("total_diff")))
                .list();
    }

    /** {@code partyColumn} koddan gelen sabit bir kolon adıdır, kullanıcı girdisi değildir. */
    private List<PartyStats> findPartyStats(String partyColumn, ReportPeriod period, ReportScope scope) {
        String sql = """
                SELECT %1$s AS party_id, count(*) AS shipments,
                       count(*) FILTER (WHERE had_discrepancy) AS discrepant,
                       coalesce(sum(abs(diff_kg)), 0) AS total_diff
                  FROM shipment_view
                 WHERE band IS NOT NULL AND %1$s IS NOT NULL%2$s
                 GROUP BY %1$s
                 ORDER BY count(*) FILTER (WHERE had_discrepancy)::numeric / count(*) DESC, count(*) DESC,
                          %1$s""".formatted(partyColumn, ScopedSql.PERIOD_FILTER + ScopedSql.SITE_FILTER);
        return ScopedSql.withPeriod(ScopedSql.bind(jdbc.sql(sql), scope), period)
                .query((rs, row) -> new PartyStats(
                        rs.getObject("party_id", UUID.class),
                        rs.getInt("shipments"),
                        rs.getInt("discrepant"),
                        rs.getBigDecimal("total_diff")))
                .list();
    }
}
