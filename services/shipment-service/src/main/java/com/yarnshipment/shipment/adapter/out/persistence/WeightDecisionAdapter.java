package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
    private static final String UPDATE_LINE = """
            UPDATE shipment_line SET net_kg = :netKg, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE shipment_id = :shipmentId AND line_no = :lineNo""";
    private static final String UPDATE_PLANNED = """
            UPDATE shipment SET planned_net_kg = :netKg, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :shipmentId""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    WeightDecisionAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public void recordDecision(UUID shipmentId, WeightDecision decision, String reason, UUID actorUserId, Instant at) {
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
    public void backToDraft(UUID shipmentId, String reason, UUID actorUserId, Instant at) {
        transitions.actAs(actorUserId);
        transitions.changeStatus(new Transition(
                shipmentId,
                ShipmentStatus.WEIGHT_MISMATCH,
                ShipmentStatus.DRAFT,
                actorUserId,
                Timestamp.from(at),
                reason));
    }

    @Override
    public void acceptRecipe(
            UUID shipmentId,
            List<RecipeLine> scaledLines,
            BigDecimal measuredKg,
            ShipmentDispatched event,
            String reason,
            UUID actorUserId,
            Instant at) {
        Timestamp when = Timestamp.from(at);
        transitions.actAs(actorUserId);
        for (RecipeLine line : scaledLines) {
            jdbc.sql(UPDATE_LINE)
                    .param("netKg", line.netKg())
                    .param("at", when)
                    .param("actor", actorUserId)
                    .param("shipmentId", shipmentId)
                    .param("lineNo", line.lineNo())
                    .update();
        }
        jdbc.sql(UPDATE_PLANNED)
                .param("netKg", measuredKg)
                .param("at", when)
                .param("actor", actorUserId)
                .param("shipmentId", shipmentId)
                .update();
        Transition transition = new Transition(
                shipmentId,
                ShipmentStatus.WEIGHT_MISMATCH,
                ShipmentStatus.AWAITING_DELIVERY,
                actorUserId,
                when,
                reason);
        transitions.changeStatus(transition);
        transitions.publish(transition, ShipmentDispatched.TYPE, event);
    }
}
