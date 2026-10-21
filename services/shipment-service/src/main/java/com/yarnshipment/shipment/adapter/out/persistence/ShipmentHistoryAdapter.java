package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentHistoryPort;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.StatusChange;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentHistoryPort}'un JDBC uygulaması. */
@Component
final class ShipmentHistoryAdapter implements ShipmentHistoryPort {

    private static final String SELECT_HISTORY = """
            SELECT from_status, to_status, changed_at, changed_by, reason
            FROM shipment_status_history
            WHERE shipment_id = ?
            ORDER BY changed_at, id""";

    private final JdbcClient jdbc;

    ShipmentHistoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<StatusChange> findHistory(UUID shipmentId) {
        return jdbc.sql(SELECT_HISTORY)
                .param(shipmentId)
                .query((rs, rowNumber) -> toStatusChange(rs))
                .list();
    }

    private static StatusChange toStatusChange(ResultSet rs) throws SQLException {
        String from = rs.getString("from_status");
        return new StatusChange(
                from == null ? null : ShipmentStatus.valueOf(from),
                ShipmentStatus.valueOf(rs.getString("to_status")),
                rs.getTimestamp("changed_at").toInstant(),
                rs.getObject("changed_by", UUID.class),
                rs.getString("reason"));
    }
}
