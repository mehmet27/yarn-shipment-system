package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.YarnReturnPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.Consumption;
import com.yarnshipment.shipment.domain.balance.MovementKind;
import com.yarnshipment.shipment.domain.fabric.ReturnDirection;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** İplik dönüşünün JDBC uygulaması: kayıt ve bakiyeden düşen hareket tek transaction'da yazılır. */
@Component
final class YarnReturnAdapter implements YarnReturnPort {

    private static final String INSERT = """
            INSERT INTO yarn_return (id, shipment_id, subcontractor_id, direction, yarn_type, lot_id, net_kg,
                                     waybill_no, created_at, created_by)
            VALUES (:id, :shipmentId, :subcontractorId, :direction, :yarnType, :lotId, :netKg, :waybillNo,
                    :createdAt, :createdBy)""";
    private static final String SELECT = """
            SELECT id, shipment_id, subcontractor_id, direction, yarn_type, lot_id, net_kg, waybill_no, created_at,
                   created_by
            FROM yarn_return WHERE shipment_id = ? ORDER BY created_at, id""";

    private final JdbcClient jdbc;
    private final BalanceMovementWriter movements;

    YarnReturnAdapter(JdbcClient jdbc, BalanceMovementWriter movements) {
        this.jdbc = jdbc;
        this.movements = movements;
    }

    @Override
    public List<YarnReturn> findByShipment(UUID shipmentId) {
        return jdbc.sql(SELECT)
                .param(shipmentId)
                .query((rs, i) -> toYarnReturn(rs))
                .list();
    }

    private static YarnReturn toYarnReturn(ResultSet rs) throws SQLException {
        return new YarnReturn(
                rs.getObject("id", UUID.class),
                rs.getObject("shipment_id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                ReturnDirection.valueOf(rs.getString("direction")),
                rs.getString("yarn_type"),
                rs.getObject("lot_id", UUID.class),
                rs.getBigDecimal("net_kg"),
                rs.getString("waybill_no"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getObject("created_by", UUID.class));
    }

    @Override
    public void save(YarnReturn yarnReturn) {
        jdbc.sql(INSERT)
                .param("id", yarnReturn.id())
                .param("shipmentId", yarnReturn.shipmentId())
                .param("subcontractorId", yarnReturn.subcontractorId())
                .param("direction", yarnReturn.direction().name())
                .param("yarnType", yarnReturn.yarnType())
                .param("lotId", yarnReturn.lotId())
                .param("netKg", yarnReturn.netKg())
                .param("waybillNo", yarnReturn.waybillNo())
                .param("createdAt", Timestamp.from(yarnReturn.createdAt()))
                .param("createdBy", yarnReturn.createdBy())
                .update();
        MovementKind kind = yarnReturn.direction() == ReturnDirection.RETURN
                ? MovementKind.YARN_RETURN
                : MovementKind.YARN_FROM_SUBCONTRACTOR;
        movements.append(BalanceMovement.consumption(new Consumption(
                kind,
                yarnReturn.subcontractorId(),
                yarnReturn.shipmentId(),
                yarnReturn.yarnType(),
                yarnReturn.lotId(),
                yarnReturn.netKg(),
                yarnReturn.createdAt(),
                yarnReturn.createdBy(),
                "YARN:" + yarnReturn.id())));
    }
}
