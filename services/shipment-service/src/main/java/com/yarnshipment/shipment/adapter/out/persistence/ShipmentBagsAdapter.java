package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentBagsPort}'un JDBC uygulaması. */
@Component
final class ShipmentBagsAdapter implements ShipmentBagsPort {

    private static final String SELECT_DISPATCH_BAGS = """
            SELECT seq_no, weight_kg, assigned_at
            FROM shipment_bag
            WHERE shipment_id = ? AND removed_at IS NULL
            ORDER BY seq_no""";

    private final JdbcClient jdbc;

    ShipmentBagsAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DispatchBag> findDispatchBags(UUID shipmentId) {
        return jdbc.sql(SELECT_DISPATCH_BAGS)
                .param(shipmentId)
                .query((rs, rowNumber) -> toDispatchBag(rs))
                .list();
    }

    private static DispatchBag toDispatchBag(ResultSet rs) throws SQLException {
        return new DispatchBag(
                rs.getInt("seq_no"),
                rs.getBigDecimal("weight_kg"),
                rs.getTimestamp("assigned_at").toInstant());
    }
}
