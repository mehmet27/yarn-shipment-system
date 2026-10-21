package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.domain.model.DeliveryScan;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.ScanMethod;
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
            SELECT bag_id, unique_key, lot_no, quality_grade_code, scan_method, assigned_at
            FROM shipment_bag
            WHERE shipment_id = ? AND removed_at IS NULL
            ORDER BY assigned_at, id""";
    private static final String SELECT_DELIVERY_SCANS = """
            SELECT s.scanned_key, s.matched, s.scan_method, s.scanned_at
            FROM delivery_scan s
            JOIN delivery_receipt r ON r.id = s.delivery_receipt_id
            WHERE r.shipment_id = ?
            ORDER BY s.scanned_at, s.id""";

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

    @Override
    public List<DeliveryScan> findDeliveryScans(UUID shipmentId) {
        return jdbc.sql(SELECT_DELIVERY_SCANS)
                .param(shipmentId)
                .query((rs, rowNumber) -> toDeliveryScan(rs))
                .list();
    }

    private static DispatchBag toDispatchBag(ResultSet rs) throws SQLException {
        return new DispatchBag(
                rs.getObject("bag_id", UUID.class),
                rs.getString("unique_key"),
                rs.getString("lot_no"),
                rs.getString("quality_grade_code"),
                ScanMethod.valueOf(rs.getString("scan_method")),
                rs.getTimestamp("assigned_at").toInstant());
    }

    private static DeliveryScan toDeliveryScan(ResultSet rs) throws SQLException {
        return new DeliveryScan(
                rs.getString("scanned_key"),
                rs.getBoolean("matched"),
                ScanMethod.valueOf(rs.getString("scan_method")),
                rs.getTimestamp("scanned_at").toInstant());
    }
}
