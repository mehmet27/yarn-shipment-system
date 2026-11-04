package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Rapor sorgularının ortak lokasyon (SEC-03) ve dönem koşulları. */
final class ScopedSql {

    /** Tablonun {@code site_id} kolonuna uygulanır. */
    static final String SITE_FILTER = " AND (:allSites OR site_id IN (:siteIds))"
            + " AND (CAST(:requestedSite AS uuid) IS NULL OR site_id = CAST(:requestedSite AS uuid))";
    /** {@code shipment_view.created_at} dönemi. */
    static final String PERIOD_FILTER = " AND created_at >= :start AND created_at < :end";

    private static final UUID NO_SITE = new UUID(0, 0);

    private ScopedSql() {}

    static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, ReportScope scope) {
        // IN () boş liste geçersiz SQL olur; allSites iken liste kullanılmaz ama yine de dolu bir değer verilir.
        List<UUID> siteIds = scope.siteIds().isEmpty() ? List.of(NO_SITE) : List.copyOf(scope.siteIds());
        UUID requested = scope.requestedSiteId();
        return spec.param("allSites", scope.allSites())
                .param("siteIds", siteIds)
                .param("requestedSite", requested == null ? null : requested.toString());
    }

    static JdbcClient.StatementSpec withPeriod(JdbcClient.StatementSpec spec, ReportPeriod period) {
        return spec.param("start", Timestamp.from(period.start())).param("end", Timestamp.from(period.endExclusive()));
    }
}
