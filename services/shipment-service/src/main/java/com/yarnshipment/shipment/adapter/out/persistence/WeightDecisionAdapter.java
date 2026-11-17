package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link WeightDecisionPort}'un JDBC uygulaması. Değişiklikler {@code audit_local} izine düşer. */
@Component
final class WeightDecisionAdapter implements WeightDecisionPort {

    private static final String RECORD_DECISION = """
            UPDATE weight_check SET decision = :decision, decided_by = :actor, decided_at = :at, reason = :reason
            WHERE id = (SELECT id FROM weight_check WHERE shipment_id = :shipmentId AND decision IS NULL
                        ORDER BY created_at DESC LIMIT 1)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    WeightDecisionAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public void recordDecision(UUID shipmentId, WeightDecision decision, String reason, Audit audit) {
        UUID actorUserId = audit.actorUserId();
        Instant at = audit.at();
        transitions.actAs(actorUserId);
        int updated = jdbc.sql(RECORD_DECISION)
                .param("decision", decision.name())
                .param("actor", actorUserId)
                .param("at", Timestamp.from(at))
                .param("reason", reason)
                .param("shipmentId", shipmentId)
                .update();
        if (updated == 0) {
            throw new InvalidShipmentStatusException(shipmentId, ShipmentStatus.WEIGHT_MISMATCH, ShipmentStatus.DRAFT);
        }
    }

    @Override
    public void backToDraft(UUID shipmentId, String reason, Audit audit) {
        UUID actorUserId = audit.actorUserId();
        Instant at = audit.at();
        transitions.actAs(actorUserId);
        transitions.changeStatus(new Transition(
                shipmentId,
                ShipmentStatus.WEIGHT_MISMATCH,
                ShipmentStatus.DRAFT,
                actorUserId,
                Timestamp.from(at),
                reason));
    }
}
