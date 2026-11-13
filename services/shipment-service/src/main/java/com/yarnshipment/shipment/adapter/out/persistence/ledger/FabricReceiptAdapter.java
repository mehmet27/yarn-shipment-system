package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.Consumption;
import com.yarnshipment.shipment.domain.balance.MovementKind;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

/** Kumaş girişinin JDBC uygulaması: kayıt, iplik dağılımı ve bakiyeden düşen hareketler tek transaction'da yazılır. */
@Component
final class FabricReceiptAdapter implements FabricReceiptPort {

    private static final String INSERT_RECEIPT = """
            INSERT INTO fabric_lot (id, source, shipment_id, fabric_type, net_kg, roll_count, waybill_no, occurred_at,
                                    created_by, yarns)
            VALUES (:id, 'SUBCONTRACTOR', :shipmentId, :fabricType, :netKg, :rollCount, :waybillNo, :receivedAt,
                    :createdBy, :yarns::jsonb)""";
    private static final String SELECT_RECEIPTS = """
            SELECT r.id, r.shipment_id, f.subcontractor_id, r.fabric_type, r.net_kg, r.roll_count, r.waybill_no,
                   r.occurred_at AS received_at, r.created_by, r.yarns
            FROM fabric_lot r JOIN fire_settlement f ON f.shipment_id = r.shipment_id
            WHERE r.shipment_id = ? AND r.source = 'SUBCONTRACTOR'
            ORDER BY r.occurred_at, r.created_at, r.id""";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;
    private final BalanceMovementWriter movements;
    private final LedgerEventPort events;

    FabricReceiptAdapter(JdbcClient jdbc, BalanceMovementWriter movements, LedgerEventPort events) {
        this.jdbc = jdbc;
        this.movements = movements;
        this.events = events;
    }

    @Override
    public List<FabricReceipt> findByShipment(UUID shipmentId) {
        return jdbc.sql(SELECT_RECEIPTS)
                .param(shipmentId)
                .query((rs, i) -> toReceipt(rs))
                .list();
    }

    private static FabricReceipt toReceipt(ResultSet rs) throws SQLException {
        return new FabricReceipt(
                rs.getObject("id", UUID.class),
                rs.getObject("shipment_id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                rs.getString("fabric_type"),
                rs.getBigDecimal("net_kg"),
                (Integer) rs.getObject("roll_count"),
                rs.getString("waybill_no"),
                rs.getTimestamp("received_at").toInstant(),
                readYarns(rs.getString("yarns")),
                rs.getObject("created_by", UUID.class));
    }

    private static List<YarnShare> readYarns(String json) {
        try {
            List<YarnShare> shares = new ArrayList<>();
            for (JsonNode node : JSON.readTree(json)) {
                shares.add(new YarnShare(
                        node.get("yarnType").asString(), node.get("netKg").decimalValue()));
            }
            shares.sort(Comparator.comparing(YarnShare::yarnType));
            return shares;
        } catch (JacksonException e) {
            throw new IllegalStateException("fabric_lot.yarns is not valid JSON", e);
        }
    }

    private static String writeYarns(List<YarnShare> yarns) {
        ArrayNode array = JSON.createArrayNode();
        for (YarnShare share : yarns) {
            array.addObject().put("yarnType", share.yarnType()).put("netKg", share.netKg());
        }
        return array.toString();
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
                .param("yarns", writeYarns(receipt.yarns()))
                .update();
        for (YarnShare share : receipt.yarns()) {
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
        events.fabricReceived(receipt);
    }
}
