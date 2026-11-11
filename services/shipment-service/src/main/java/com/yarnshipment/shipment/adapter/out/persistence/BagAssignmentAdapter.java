package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.BagAssignmentPort;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link BagAssignmentPort}'un JDBC uygulaması. Çuvallar sevkiyattaki son sıra numarasından devam eden numaralarla
 * eklenir; çuval anahtarı yoktur, {@code unique_key} sıra numarasının metnidir.
 */
@Component
final class BagAssignmentAdapter implements BagAssignmentPort {

    private static final String LAST_SEQ =
            "SELECT COALESCE(max(seq_no), 0) FROM shipment_bag WHERE shipment_id = :shipmentId AND removed_at IS NULL";
    private static final String INSERT = """
            INSERT INTO shipment_bag (id, shipment_id, bag_id, unique_key, seq_no, weight_kg, package_type,
                                      scan_method, assigned_at, assigned_by, created_at, created_by)
            VALUES (:id, :shipmentId, :bagId, :uniqueKey, :seqNo, :weightKg, 'BAG', 'MANUAL', :at, :actor, :at,
                    :actor)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    BagAssignmentAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public int assign(BagAssignment assignment, UUID actorUserId, Instant at) {
        transitions.actAs(actorUserId);
        Timestamp now = Timestamp.from(at);
        int seqNo = jdbc.sql(LAST_SEQ)
                .param("shipmentId", assignment.shipmentId())
                .query(Integer.class)
                .single();
        int inserted = 0;
        for (BigDecimal weightKg : assignment.weightsKg()) {
            seqNo++;
            inserted += jdbc.sql(INSERT)
                    .param("id", transitions.nextId())
                    .param("shipmentId", assignment.shipmentId())
                    .param("bagId", transitions.nextId())
                    .param("uniqueKey", String.valueOf(seqNo))
                    .param("seqNo", seqNo)
                    .param("weightKg", weightKg)
                    .param("at", now)
                    .param("actor", actorUserId)
                    .update();
        }
        return inserted;
    }
}
