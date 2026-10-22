package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.BagAssignmentPort;
import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link BagAssignmentPort}'un JDBC uygulaması. Sevkiyatta zaten olan çuval atlanır. */
@Component
final class BagAssignmentAdapter implements BagAssignmentPort {

    private static final String INSERT = """
            INSERT INTO shipment_bag (id, shipment_id, bag_id, unique_key, lot_no, quality_grade_code, scan_method,
                                      assigned_at, assigned_by, created_at, created_by)
            VALUES (:id, :shipmentId, :bagId, :uniqueKey, :lotNo, :grade, :method, :at, :actor, :at, :actor)
            ON CONFLICT (shipment_id, bag_id) DO NOTHING""";

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
        int inserted = 0;
        for (ReservedBag bag : assignment.bags()) {
            inserted += jdbc.sql(INSERT)
                    .param("id", transitions.nextId())
                    .param("shipmentId", assignment.shipmentId())
                    .param("bagId", bag.bagId())
                    .param("uniqueKey", bag.uniqueKey())
                    .param("lotNo", bag.lotNo())
                    .param("grade", bag.qualityGradeCode())
                    .param("method", assignment.scanMethod().name())
                    .param("at", now)
                    .param("actor", actorUserId)
                    .update();
        }
        return inserted;
    }
}
