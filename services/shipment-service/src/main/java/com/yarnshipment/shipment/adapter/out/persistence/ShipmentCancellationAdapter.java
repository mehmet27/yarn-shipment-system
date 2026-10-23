package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.ShipmentCancellationPort;
import com.yarnshipment.shipment.domain.cancellation.Cancellation;
import com.yarnshipment.shipment.domain.cancellation.ShipmentCancelled;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link ShipmentCancellationPort}'un JDBC uygulaması. Gerekçe önce yazılır (durum CANCELLED olunca zorunlu), sonra
 * durum geçer; sevkiyattaki çuvallar çıkarılmış işaretlenir ve outbox olayı yazılır.
 */
@Component
final class ShipmentCancellationAdapter implements ShipmentCancellationPort {

    private static final String SET_REASON =
            "UPDATE shipment SET cancelled_reason = :reason WHERE id = :id AND status = :status";
    private static final String REMOVE_BAGS = """
            UPDATE shipment_bag
            SET removed_at = :at, removed_by = :actor, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE shipment_id = :id AND removed_at IS NULL""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    ShipmentCancellationAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public void cancel(Cancellation cancellation, UUID actorUserId) {
        Timestamp at = Timestamp.from(cancellation.cancelledAt());
        transitions.actAs(actorUserId);
        int marked = jdbc.sql(SET_REASON)
                .param("reason", cancellation.reason())
                .param("id", cancellation.shipmentId())
                .param("status", cancellation.fromStatus().name())
                .update();
        if (marked == 0) {
            throw new InvalidShipmentStatusException(
                    cancellation.shipmentId(), cancellation.fromStatus(), cancellation.toStatus());
        }
        Transition transition = new Transition(
                cancellation.shipmentId(),
                cancellation.fromStatus(),
                cancellation.toStatus(),
                actorUserId,
                at,
                cancellation.reason());
        transitions.changeStatus(transition);
        jdbc.sql(REMOVE_BAGS)
                .param("at", at)
                .param("actor", actorUserId)
                .param("id", cancellation.shipmentId())
                .update();
        transitions.publish(transition, ShipmentCancelled.TYPE, ShipmentCancelled.of(cancellation));
    }
}
