package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Sayfalı ana veri sorgusunu çalıştırır: sayım, sıralı sayfa ve LIKE kaçışlı arama deseni. */
final class PagedQuery {

    /** Kullanılmayan {@code :siteIds} için geçerli, hiçbir kayda uymayan değer. */
    static final List<UUID> NO_SITES = List.of(new UUID(0, 0));

    private final JdbcClient jdbc;

    PagedQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    <T> MasterDataPage<T> empty(MasterDataQuery query) {
        return new MasterDataPage<>(List.of(), query.page(), query.size(), 0);
    }

    <T> MasterDataPage<T> page(Select select, MasterDataQuery query, Filters filters, Mapper<T> mapper) {
        String pattern = pattern(query.text());
        long total = bind(jdbc.sql("SELECT count(*) " + select.from()), pattern, filters, query.includeInactive())
                .query(Long.class)
                .single();
        String sql = "SELECT " + select.columns() + " " + select.from() + " ORDER BY " + select.orderBy()
                + ", id LIMIT :limit OFFSET :offset";
        List<T> items = bind(jdbc.sql(sql), pattern, filters, query.includeInactive())
                .param("limit", query.size())
                .param("offset", (long) query.page() * query.size())
                .query((rs, rowNumber) -> mapper.apply(rs))
                .list();
        return new MasterDataPage<>(items, query.page(), query.size(), total);
    }

    private static JdbcClient.StatementSpec bind(
            JdbcClient.StatementSpec spec, @Nullable String pattern, Filters filters, boolean includeInactive) {
        return spec.param("pattern", pattern)
                .param("includeInactive", includeInactive)
                .param("subcontractorId", filters.subcontractorId())
                .param("allSites", filters.allSites())
                .param("siteIds", filters.siteIds());
    }

    private static @Nullable String pattern(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String escaped = text.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Bir liste sorgusunun parçaları. */
    record Select(String from, String orderBy, String columns) {}

    /** Sorguya bağlanan, bazı sorgularda kullanılmayan süzgeçler. */
    record Filters(@Nullable UUID subcontractorId, boolean allSites, List<UUID> siteIds) {}

    @FunctionalInterface
    interface Mapper<T> {
        T apply(ResultSet rs) throws SQLException;
    }
}
