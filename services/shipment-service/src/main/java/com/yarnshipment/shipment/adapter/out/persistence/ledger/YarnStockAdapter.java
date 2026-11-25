package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.YarnStockPort;
import com.yarnshipment.shipment.domain.fabric.YarnStockAllocator;
import com.yarnshipment.shipment.domain.fabric.YarnStockAllocator.Bucket;
import com.yarnshipment.shipment.domain.fabric.YarnStockRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link YarnStockPort}'un JDBC uygulaması: fire dosyası kapanmamış sevkiyatların defter toplamları, sevkiyat, iplik ve
 * partiye göre. Defterde iplik tanım kimliği ya da ad olarak yazılabildiği için ad, tanımdan çözülür.
 */
@Component
final class YarnStockAdapter implements YarnStockPort {

    private static final String BUCKETS = """
            SELECT s.id AS shipment_id, s.shipment_no, coalesce(s.locked_at, f.created_at) AS since,
                   m.yarn_type, coalesce(p.name, m.yarn_type) AS yarn_name, m.lot_id, pl.lot_no,
                   sup.name AS supplier_name, sum(m.net_kg) AS net_kg, min(m.occurred_at) AS first_at
            FROM fire_settlement f
            JOIN shipment s ON s.id = f.shipment_id
            JOIN balance_movement m ON m.shipment_id = f.shipment_id AND m.subcontractor_id = f.subcontractor_id
            LEFT JOIN md_product p ON p.id::text = m.yarn_type
            LEFT JOIN md_production_lot pl ON pl.id = m.lot_id
            LEFT JOIN md_yarn_supplier sup ON sup.id = pl.yarn_supplier_id
            WHERE f.subcontractor_id = :subcontractorId AND f.status IN ('AWAITING_FABRIC', 'TO_INVOICE')
            GROUP BY s.id, s.shipment_no, since, m.yarn_type, yarn_name, m.lot_id, pl.lot_no, sup.name
            ORDER BY since, s.id, yarn_name, m.lot_id""";

    private final JdbcClient jdbc;

    YarnStockAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<YarnStockRow> findOpenStock(UUID subcontractorId) {
        List<Bucket> buckets = jdbc.sql(BUCKETS)
                .param("subcontractorId", subcontractorId)
                .query((rs, i) -> toBucket(rs))
                .list();
        return YarnStockAllocator.allocate(buckets);
    }

    private static Bucket toBucket(ResultSet rs) throws SQLException {
        return new Bucket(
                rs.getObject("shipment_id", UUID.class),
                rs.getString("shipment_no"),
                rs.getTimestamp("since").toInstant(),
                rs.getString("yarn_type"),
                rs.getString("yarn_name"),
                rs.getObject("lot_id", UUID.class),
                rs.getString("lot_no"),
                rs.getString("supplier_name"),
                rs.getBigDecimal("net_kg"),
                rs.getTimestamp("first_at").toInstant());
    }
}
