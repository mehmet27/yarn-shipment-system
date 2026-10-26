package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery.Filter;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link DiscrepancyQueryPort}'un JDBC uygulaması. Kapsam ve süzgeçler WHERE koşuluna çevrilir. */
@Component
final class DiscrepancyQueryAdapter implements DiscrepancyQueryPort {

    private static final String SELECT = """
            SELECT id, discrepancy_no, site_id, shipment_id, discrepancy_type, severity, status, rule_code,
                   expected_value, actual_value, diff_kg, diff_pct, assignee_id, due_at, resolution, resolution_note,
                   liable_party, resolved_by, resolved_at, created_at
              FROM discrepancy""";
    private static final String ORDER = " ORDER BY due_at NULLS LAST, severity DESC, discrepancy_no";
    private static final String ACTIVITY = """
            SELECT id, action_type, note, evidence_id, actor_id, occurred_at
              FROM discrepancy_activity
             WHERE discrepancy_id = :discrepancyId
             ORDER BY occurred_at, id""";

    private final JdbcClient jdbc;

    DiscrepancyQueryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Discrepancy> findDiscrepancy(UUID discrepancyId) {
        return jdbc.sql(SELECT + " WHERE id = :id")
                .param("id", discrepancyId)
                .query(DiscrepancyRows::map)
                .optional();
    }

    @Override
    public DiscrepancyPage findDiscrepancies(Filter filter, AccessScope scope, int page, int size) {
        Where where = new Where();
        if (!scope.allSites()) {
            where.add("site_id IN (:siteIds)", "siteIds", scope.siteIds());
        }
        where.add("status = :status", "status", name(filter.status()));
        where.add("severity = :severity", "severity", name(filter.severity()));
        where.add("assignee_id = :assigneeId", "assigneeId", filter.assigneeId());
        where.add("shipment_id = :shipmentId", "shipmentId", filter.shipmentId());

        long total = where.bind(jdbc.sql("SELECT count(*) FROM discrepancy" + where.clause()))
                .query(Long.class)
                .single();
        List<Discrepancy> items = where.bind(jdbc.sql(SELECT + where.clause() + ORDER + " LIMIT :limit OFFSET :offset"))
                .param("limit", size)
                .param("offset", (long) page * size)
                .query(DiscrepancyRows::map)
                .list();
        return new DiscrepancyPage(items, page, size, total);
    }

    @Override
    public List<DiscrepancyActivity> findActivity(UUID discrepancyId) {
        return jdbc.sql(ACTIVITY)
                .param("discrepancyId", discrepancyId)
                .query(DiscrepancyRows::mapActivity)
                .list();
    }

    private static @Nullable String name(@Nullable Enum<?> value) {
        return value == null ? null : value.name();
    }

    /** Dolu süzgeçleri WHERE koşuluna ve adlı parametrelere çevirir; boş süzgeç koşul eklemez. */
    private static final class Where {

        private final List<String> conditions = new ArrayList<>();
        private final Map<String, Object> params = new LinkedHashMap<>();

        void add(String condition, String name, @Nullable Object value) {
            if (value != null) {
                conditions.add(condition);
                params.put(name, value);
            }
        }

        String clause() {
            return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        }

        JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement) {
            JdbcClient.StatementSpec bound = statement;
            for (Map.Entry<String, Object> param : params.entrySet()) {
                bound = bound.param(param.getKey(), param.getValue());
            }
            return bound;
        }
    }
}
