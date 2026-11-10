package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.CompletionPort;
import com.yarnshipment.shipment.domain.completion.Completion;
import com.yarnshipment.shipment.domain.completion.ShipmentCompleted;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link CompletionPort}'un JDBC uygulaması. İrsaliye numarası ve belgesi önce yazılır, sonra durum geçer ve
 * sevkiyat kilitlenir: bağlı kayıtlar artık yalnızca Admin'in gerekçeli düzeltmesiyle değişir.
 */
@Component
final class CompletionAdapter implements CompletionPort {

    private static final String SET_WAYBILL =
            "UPDATE shipment SET e_waybill_no = :waybillNo, waybill_evidence_id = :evidenceId"
                    + " WHERE id = :id AND status = :status";
    private static final String LOCK = "UPDATE shipment SET locked_at = ? WHERE id = ? AND locked_at IS NULL";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    CompletionAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public void complete(Completion completion, UUID actorUserId) {
        Timestamp at = Timestamp.from(completion.completedAt());
        transitions.actAs(actorUserId);
        int marked = jdbc.sql(SET_WAYBILL)
                .param("waybillNo", completion.waybillNo())
                .param("evidenceId", completion.waybillEvidenceId())
                .param("id", completion.shipmentId())
                .param("status", completion.fromStatus().name())
                .update();
        if (marked == 0) {
            throw new InvalidShipmentStatusException(
                    completion.shipmentId(), completion.fromStatus(), completion.toStatus());
        }
        Transition transition = new Transition(
                completion.shipmentId(), completion.fromStatus(), completion.toStatus(), actorUserId, at);
        transitions.changeStatus(transition);
        jdbc.sql(LOCK).params(at, completion.shipmentId()).update();
        transitions.publish(transition, ShipmentCompleted.TYPE, ShipmentCompleted.of(completion));
    }
}
