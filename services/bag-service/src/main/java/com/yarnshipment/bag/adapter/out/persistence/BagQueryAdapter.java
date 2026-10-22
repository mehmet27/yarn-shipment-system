package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.PackageType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link BagQueryPort}'un JDBC uygulaması. Açık rezervasyon varsa sevkiyatı çuvalla birlikte okunur. */
@Component
final class BagQueryAdapter implements BagQueryPort {

    private static final String SELECT = """
            SELECT b.id, b.site_id, b.unique_key, b.package_type, b.lot_no, b.quality_grade_code, b.status,
                   r.shipment_id
            FROM bag b
            LEFT JOIN bag_reservation r ON r.bag_id = b.id AND r.released_at IS NULL
            """;

    private final JdbcClient jdbc;

    BagQueryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Bag> findByUniqueKey(String uniqueKey) {
        return jdbc.sql(SELECT + "WHERE b.unique_key = :key")
                .param("key", uniqueKey)
                .query((rs, rowNumber) -> map(rs))
                .optional();
    }

    @Override
    public List<Bag> findByUniqueKeys(Collection<String> uniqueKeys) {
        if (uniqueKeys.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(SELECT + "WHERE b.unique_key IN (:keys) ORDER BY b.unique_key")
                .param("keys", List.copyOf(uniqueKeys))
                .query((rs, rowNumber) -> map(rs))
                .list();
    }

    @Override
    public List<Bag> findReservedFor(UUID shipmentId) {
        return jdbc.sql(SELECT + "WHERE r.shipment_id = :shipmentId ORDER BY b.unique_key")
                .param("shipmentId", shipmentId)
                .query((rs, rowNumber) -> map(rs))
                .list();
    }

    private static Bag map(ResultSet rs) throws SQLException {
        return new Bag(
                rs.getObject("id", UUID.class),
                rs.getObject("site_id", UUID.class),
                rs.getString("unique_key"),
                PackageType.valueOf(rs.getString("package_type")),
                rs.getString("lot_no"),
                rs.getString("quality_grade_code"),
                BagStatus.valueOf(rs.getString("status")),
                rs.getObject("shipment_id", UUID.class));
    }
}
