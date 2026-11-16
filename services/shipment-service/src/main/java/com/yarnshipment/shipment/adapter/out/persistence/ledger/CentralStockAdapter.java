package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.YarnFlow;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockKind;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Merkez iplik stoku defterinin JDBC uygulaması; üretim ve tükettiği iplikler tek transaction'da yazılır. */
@Component
final class CentralStockAdapter implements CentralStockPort {

    private static final String INSERT_MOVEMENT = """
            INSERT INTO central_stock_movement (id, product_id, yarn_type, kind, net_kg, production_id, fabric_kg, note,
                                                occurred_at, created_by, source_ref)
            VALUES (:id, :productId, :yarnType, :kind, :netKg, :productionId, :fabricKg, :note, :occurredAt, :createdBy,
                    :sourceRef)""";
    private static final String INSERT_PRODUCTION = """
            INSERT INTO fabric_lot (id, source, fabric_type, net_kg, fire_rate, roll_count, waybill_no, occurred_at,
                                    created_by)
            VALUES (:id, 'CENTRAL', :fabricType, :netKg, :fireRate, :rollCount, :waybillNo, :producedAt, :createdBy)""";
    private static final String STOCK = """
            SELECT m.product_id, coalesce(p.name, m.yarn_type) AS yarn_type, sum(m.net_kg) AS stock_kg
            FROM central_stock_movement m
            LEFT JOIN md_product p ON p.id = m.product_id
            GROUP BY m.product_id, coalesce(p.name, m.yarn_type)
            ORDER BY coalesce(p.name, m.yarn_type), m.product_id""";
    private static final String SHORTAGES = """
            SELECT st.product_id, p.name AS yarn_type, st.stock_kg
            FROM (SELECT product_id, sum(net_kg) AS stock_kg
                  FROM central_stock_movement
                  WHERE product_id IN (SELECT product_id FROM central_stock_movement
                                       WHERE shipment_id = :shipmentId AND kind = 'TO_SUBCONTRACTOR'
                                         AND product_id IS NOT NULL)
                  GROUP BY product_id) st
            JOIN md_product p ON p.id = st.product_id
            WHERE st.stock_kg < 0
            ORDER BY p.name""";
    private static final String ACTIVE_PRODUCTS = "SELECT id, name FROM md_product WHERE active AND id IN (:ids)";
    private static final String PRODUCED = "SELECT coalesce(sum(net_kg), 0) FROM fabric_lot WHERE source = 'CENTRAL'";
    private static final String FLOW = """
            SELECT
                (SELECT coalesce(sum(net_kg), 0) FROM central_stock_movement
                  WHERE kind = 'YARN_RECEIPT') AS received_kg,
                (SELECT coalesce(sum(net_kg), 0) FROM central_stock_movement
                  WHERE kind = 'YARN_RECEIPT' AND source_ref LIKE 'DIRECT_IN:%') AS direct_kg,
                (SELECT coalesce(sum(net_kg), 0) FROM balance_movement) AS at_subcontractors_kg,
                (SELECT coalesce(-sum(net_kg), 0) FROM central_stock_movement
                  WHERE kind = 'PRODUCTION') AS used_kg""";
    private static final String RECENT = """
            SELECT m.id, m.product_id, coalesce(p.name, m.yarn_type) AS yarn_type, m.kind, m.net_kg, m.production_id,
                   m.fabric_kg, m.note, m.occurred_at, m.created_by
            FROM central_stock_movement m LEFT JOIN md_product p ON p.id = m.product_id
            ORDER BY m.occurred_at DESC, m.id DESC LIMIT ?""";

    private final JdbcClient jdbc;

    CentralStockAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveReceipt(CentralStockMovement receipt) {
        insert(receipt, "RECEIPT:" + receipt.id());
    }

    @Override
    public void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions) {
        jdbc.sql(INSERT_PRODUCTION)
                .param("id", production.id())
                .param("fabricType", production.fabricType())
                .param("netKg", production.netKg())
                .param("fireRate", production.fireRate())
                .param("rollCount", production.rollCount())
                .param("waybillNo", production.waybillNo())
                .param("producedAt", Timestamp.from(production.producedAt()))
                .param("createdBy", production.createdBy())
                .update();
        consumptions.forEach(movement -> insert(movement, "PRODUCTION:" + movement.id()));
    }

    @Override
    public List<CentralStockRow> findStock() {
        return jdbc.sql(STOCK)
                .query((rs, i) -> new CentralStockRow(
                        rs.getObject("product_id", UUID.class),
                        rs.getString("yarn_type"),
                        rs.getBigDecimal("stock_kg")))
                .list();
    }

    @Override
    public List<CentralStockRow> findShortagesAfterDispatch(UUID shipmentId) {
        return jdbc.sql(SHORTAGES)
                .param("shipmentId", shipmentId)
                .query((rs, i) -> new CentralStockRow(
                        rs.getObject("product_id", UUID.class),
                        rs.getString("yarn_type"),
                        rs.getBigDecimal("stock_kg")))
                .list();
    }

    @Override
    public Map<UUID, String> findActiveProductNames(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        jdbc.sql(ACTIVE_PRODUCTS)
                .param("ids", List.copyOf(productIds))
                .query((rs, i) -> {
                    names.put(rs.getObject("id", UUID.class), rs.getString("name"));
                    return null;
                })
                .list();
        return names;
    }

    @Override
    public BigDecimal findProducedFabricKg() {
        return jdbc.sql(PRODUCED).query(BigDecimal.class).single();
    }

    @Override
    public YarnFlow findYarnFlow() {
        return jdbc.sql(FLOW)
                .query((rs, i) -> new YarnFlow(
                        rs.getBigDecimal("received_kg"),
                        rs.getBigDecimal("direct_kg"),
                        rs.getBigDecimal("at_subcontractors_kg"),
                        rs.getBigDecimal("used_kg")))
                .single();
    }

    @Override
    public List<CentralStockMovement> findRecent(int limit) {
        return jdbc.sql(RECENT).param(limit).query((rs, i) -> toMovement(rs)).list();
    }

    private static CentralStockMovement toMovement(ResultSet rs) throws SQLException {
        return new CentralStockMovement(
                rs.getObject("id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getString("yarn_type"),
                CentralStockKind.valueOf(rs.getString("kind")),
                rs.getBigDecimal("net_kg"),
                rs.getObject("production_id", UUID.class),
                rs.getBigDecimal("fabric_kg"),
                rs.getString("note"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getObject("created_by", UUID.class));
    }

    private void insert(CentralStockMovement movement, String sourceRef) {
        jdbc.sql(INSERT_MOVEMENT)
                .param("id", movement.id())
                .param("productId", movement.productId())
                .param("yarnType", movement.yarnType())
                .param("kind", movement.kind().name())
                .param("netKg", movement.netKg())
                .param("productionId", movement.productionId())
                .param("fabricKg", movement.fabricKg())
                .param("note", movement.note())
                .param("occurredAt", Timestamp.from(movement.occurredAt()))
                .param("createdBy", movement.createdBy())
                .param("sourceRef", sourceRef)
                .update();
    }
}
