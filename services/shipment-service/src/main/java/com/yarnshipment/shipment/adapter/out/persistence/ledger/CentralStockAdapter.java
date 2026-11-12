package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockKind;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Merkez iplik stoku defterinin JDBC uygulaması; üretim ve tükettiği iplikler tek transaction'da yazılır. */
@Component
final class CentralStockAdapter implements CentralStockPort {

    private static final String INSERT_MOVEMENT = """
            INSERT INTO central_stock_movement (id, yarn_type, kind, net_kg, production_id, fabric_kg, note,
                                                occurred_at, created_by, source_ref)
            VALUES (:id, :yarnType, :kind, :netKg, :productionId, :fabricKg, :note, :occurredAt, :createdBy,
                    :sourceRef)""";
    private static final String INSERT_PRODUCTION = """
            INSERT INTO central_production (id, fabric_type, net_kg, fire_rate, roll_count, waybill_no, produced_at,
                                            created_by)
            VALUES (:id, :fabricType, :netKg, :fireRate, :rollCount, :waybillNo, :producedAt, :createdBy)""";
    private static final String STOCK = """
            SELECT yarn_type, sum(net_kg) AS stock_kg FROM central_stock_movement
            GROUP BY yarn_type ORDER BY yarn_type""";
    private static final String PRODUCED = "SELECT coalesce(sum(net_kg), 0) FROM central_production";
    private static final String RECENT = """
            SELECT id, yarn_type, kind, net_kg, production_id, fabric_kg, note, occurred_at, created_by
            FROM central_stock_movement ORDER BY occurred_at DESC, id DESC LIMIT ?""";

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
                .query((rs, i) -> new CentralStockRow(rs.getString("yarn_type"), rs.getBigDecimal("stock_kg")))
                .list();
    }

    @Override
    public BigDecimal findProducedFabricKg() {
        return jdbc.sql(PRODUCED).query(BigDecimal.class).single();
    }

    @Override
    public List<CentralStockMovement> findRecent(int limit) {
        return jdbc.sql(RECENT).param(limit).query((rs, i) -> toMovement(rs)).list();
    }

    private static CentralStockMovement toMovement(ResultSet rs) throws SQLException {
        return new CentralStockMovement(
                rs.getObject("id", UUID.class),
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
