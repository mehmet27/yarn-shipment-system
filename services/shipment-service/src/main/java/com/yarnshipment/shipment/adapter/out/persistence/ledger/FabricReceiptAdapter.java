package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.Consumption;
import com.yarnshipment.shipment.domain.balance.MovementKind;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Kumaş girişinin JDBC uygulaması: kayıt, iplik dağılımı ve bakiyeden düşen hareketler tek transaction'da yazılır. */
@Component
final class FabricReceiptAdapter implements FabricReceiptPort {

    private static final String INSERT_RECEIPT = """
            INSERT INTO fabric_receipt (id, shipment_id, fabric_type, net_kg, roll_count, waybill_no, received_at,
                                        created_by)
            VALUES (:id, :shipmentId, :fabricType, :netKg, :rollCount, :waybillNo, :receivedAt, :createdBy)""";
    private static final String INSERT_YARN =
            "INSERT INTO fabric_receipt_yarn (fabric_receipt_id, yarn_type, net_kg) VALUES (?, ?, ?)";
    private static final String SELECT_RECEIPTS = """
            SELECT r.id, r.shipment_id, f.subcontractor_id, r.fabric_type, r.net_kg, r.roll_count, r.waybill_no,
                   r.received_at, r.created_by
            FROM fabric_receipt r JOIN fire_settlement f ON f.shipment_id = r.shipment_id
            WHERE r.shipment_id = ? ORDER BY r.received_at, r.created_at, r.id""";
    private static final String SELECT_YARNS = """
            SELECT y.fabric_receipt_id, y.yarn_type, y.net_kg
            FROM fabric_receipt_yarn y JOIN fabric_receipt r ON r.id = y.fabric_receipt_id
            WHERE r.shipment_id = ? ORDER BY y.yarn_type""";

    private final JdbcClient jdbc;
    private final BalanceMovementWriter movements;

    FabricReceiptAdapter(JdbcClient jdbc, BalanceMovementWriter movements) {
        this.jdbc = jdbc;
        this.movements = movements;
    }

    @Override
    public List<FabricReceipt> findByShipment(UUID shipmentId) {
        Map<UUID, List<YarnShare>> yarnsByReceipt = new LinkedHashMap<>();
        jdbc.sql(SELECT_YARNS)
                .param(shipmentId)
                .query((rs, i) -> yarnsByReceipt
                        .computeIfAbsent(rs.getObject("fabric_receipt_id", UUID.class), id -> new ArrayList<>())
                        .add(new YarnShare(rs.getString("yarn_type"), rs.getBigDecimal("net_kg"))))
                .list();
        return jdbc.sql(SELECT_RECEIPTS)
                .param(shipmentId)
                .query((rs, i) -> toReceipt(rs, yarnsByReceipt))
                .list();
    }

    private static FabricReceipt toReceipt(ResultSet rs, Map<UUID, List<YarnShare>> yarnsByReceipt)
            throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new FabricReceipt(
                id,
                rs.getObject("shipment_id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                rs.getString("fabric_type"),
                rs.getBigDecimal("net_kg"),
                (Integer) rs.getObject("roll_count"),
                rs.getString("waybill_no"),
                rs.getTimestamp("received_at").toInstant(),
                yarnsByReceipt.getOrDefault(id, List.of()),
                rs.getObject("created_by", UUID.class));
    }

    @Override
    public void save(FabricReceipt receipt) {
        jdbc.sql(INSERT_RECEIPT)
                .param("id", receipt.id())
                .param("shipmentId", receipt.shipmentId())
                .param("fabricType", receipt.fabricType())
                .param("netKg", receipt.netKg())
                .param("rollCount", receipt.rollCount())
                .param("waybillNo", receipt.waybillNo())
                .param("receivedAt", Timestamp.from(receipt.receivedAt()))
                .param("createdBy", receipt.createdBy())
                .update();
        for (YarnShare share : receipt.yarns()) {
            jdbc.sql(INSERT_YARN)
                    .params(receipt.id(), share.yarnType(), share.netKg())
                    .update();
            movements.append(BalanceMovement.consumption(new Consumption(
                    MovementKind.FABRIC_RECEIPT,
                    receipt.subcontractorId(),
                    receipt.shipmentId(),
                    share.yarnType(),
                    null,
                    share.netKg(),
                    receipt.receivedAt(),
                    receipt.createdBy(),
                    "FABRIC:" + receipt.id() + ":" + share.yarnType())));
        }
    }
}
