package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.fabric.FabricDrawEntry;
import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.Consumption;
import com.yarnshipment.shipment.domain.balance.MovementKind;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.FabricYarn;
import com.yarnshipment.shipment.domain.fabric.YarnDraw;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Kumaş girişinin JDBC uygulaması: kayıt, iplik oranları, sevkiyatlardan iplik düşümleri ve bakiyeden düşen hareketler
 * tek transaction'da yazılır; okuma {@link FabricReceiptReader}'dadır.
 */
@Component
final class FabricReceiptAdapter implements FabricReceiptPort {

    private static final String INSERT_RECEIPT = """
            INSERT INTO fabric_lot (id, source, shipment_id, fabric_type, net_kg, roll_count, waybill_no, occurred_at,
                                    created_by, yarns)
            VALUES (:id, 'SUBCONTRACTOR', :shipmentId, :fabricType, :netKg, :rollCount, :waybillNo, :receivedAt,
                    :createdBy, :yarns::jsonb)""";
    private static final String INSERT_DRAW = """
            INSERT INTO fabric_yarn_draw (id, fabric_lot_id, source_shipment_id, yarn_type, lot_id, net_kg, position)
            VALUES (:id, :fabricId, :shipmentId, :yarnType, :lotId, :netKg, :position)""";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;
    private final BalanceMovementWriter movements;
    private final LedgerEventPort events;
    private final FabricReceiptReader reader;

    FabricReceiptAdapter(
            JdbcClient jdbc, BalanceMovementWriter movements, LedgerEventPort events, FabricReceiptReader reader) {
        this.jdbc = jdbc;
        this.movements = movements;
        this.events = events;
        this.reader = reader;
    }

    @Override
    public List<FabricReceipt> findByShipment(UUID shipmentId) {
        return reader.findByShipment(shipmentId);
    }

    @Override
    public List<FabricDrawEntry> findDrawsFrom(UUID sourceShipmentId) {
        return reader.findDrawsFrom(sourceShipmentId);
    }

    private static String writeYarns(List<FabricYarn> yarns) {
        ArrayNode array = JSON.createArrayNode();
        for (FabricYarn yarn : yarns) {
            ObjectNode node = array.addObject();
            node.put("yarnType", yarn.yarnType());
            if (yarn.lotId() != null) {
                node.put("lotId", yarn.lotId().toString());
            }
            node.put("ratioPct", yarn.ratioPct());
            node.put("netKg", yarn.netKg());
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
        int position = 0;
        for (FabricYarn yarn : receipt.yarns()) {
            for (YarnDraw draw : yarn.draws()) {
                writeDraw(receipt, draw, position++);
            }
        }
        events.fabricReceived(receipt);
    }

    /** Düşüm satırı ve bakiye hareketi; hareket iplikin düşüldüğü sevkiyata yazılır. */
    private void writeDraw(FabricReceipt receipt, YarnDraw draw, int position) {
        UUID drawId = UUID.randomUUID();
        jdbc.sql(INSERT_DRAW)
                .param("id", drawId)
                .param("fabricId", receipt.id())
                .param("shipmentId", draw.shipmentId())
                .param("yarnType", draw.rawYarnType())
                .param("lotId", draw.lotId())
                .param("netKg", draw.netKg())
                .param("position", position)
                .update();
        movements.append(BalanceMovement.consumption(new Consumption(
                MovementKind.FABRIC_RECEIPT,
                receipt.subcontractorId(),
                draw.shipmentId(),
                draw.rawYarnType(),
                draw.lotId(),
                draw.netKg(),
                receipt.receivedAt(),
                receipt.createdBy(),
                "FABRIC:" + receipt.id() + ":" + drawId)));
    }
}
