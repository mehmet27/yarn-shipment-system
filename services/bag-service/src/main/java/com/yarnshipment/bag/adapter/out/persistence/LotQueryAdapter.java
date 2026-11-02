package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.LockedLot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link LotQueryPort}'un JDBC uygulaması; çuval sayısı aynı sorguda sayılır. */
@Component
final class LotQueryAdapter implements LotQueryPort {

    private static final String SELECT = """
            SELECT l.lot_id, l.site_id, l.lot_no, l.product_id, l.quality_grade_code, l.locked_at,
                   (SELECT count(*) FROM bag b WHERE b.lot_id = l.lot_id) AS bag_count
            FROM lot_quality_lock l
            """;

    private final JdbcClient jdbc;

    LotQueryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LockedLot> findLocked(UUID lotId) {
        return jdbc.sql(SELECT + "WHERE l.lot_id = :lotId")
                .param("lotId", lotId)
                .query((rs, rowNumber) -> map(rs))
                .optional();
    }

    @Override
    public List<LockedLot> findLockedInSites(Collection<UUID> siteIds) {
        if (siteIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(SELECT + "WHERE l.site_id IN (:siteIds) ORDER BY l.locked_at DESC, l.lot_no")
                .param("siteIds", List.copyOf(siteIds))
                .query((rs, rowNumber) -> map(rs))
                .list();
    }

    private static LockedLot map(ResultSet rs) throws SQLException {
        return new LockedLot(
                rs.getObject("lot_id", UUID.class),
                rs.getObject("site_id", UUID.class),
                rs.getString("lot_no"),
                rs.getObject("product_id", UUID.class),
                rs.getString("quality_grade_code"),
                rs.getTimestamp("locked_at").toInstant(),
                rs.getInt("bag_count"));
    }
}
