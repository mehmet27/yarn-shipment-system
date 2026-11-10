package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.DispatchCommandPort;
import com.yarnshipment.shipment.domain.model.DispatchRecording;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.Weighing.TareBasis;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link DispatchCommandPort}'un JDBC uygulaması: tartım, durum geçişi ve outbox olayı aynı transaction'da. */
@Component
final class DispatchCommandAdapter implements DispatchCommandPort {

    private static final String COUNT_WEIGHINGS =
            "SELECT count(*) FROM weighing WHERE shipment_id = ? AND side = 'DISPATCH' AND superseded_by IS NULL";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, scale_ticket_no, weighed_at,
                                  entered_by, created_at, created_by, expected_tare_kg, tare_tolerance_pct)
            VALUES (:id, :shipmentId, 'DISPATCH', :grossKg, :tareKg, :bagCount, :ticketNo, :at, :actor, :at, :actor,
                    :expectedTare, :tolerance)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    DispatchCommandAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public boolean hasDispatchWeighing(UUID shipmentId) {
        long count =
                jdbc.sql(COUNT_WEIGHINGS).param(shipmentId).query(Long.class).single();
        return count > 0;
    }

    @Override
    public void recordDispatch(DispatchRecording recording, UUID actorUserId) {
        Transition transition = new Transition(
                recording.shipmentId(),
                recording.fromStatus(),
                recording.toStatus(),
                actorUserId,
                Timestamp.from(recording.recordedAt()));
        transitions.actAs(actorUserId);
        transitions.changeStatus(transition);
        insertWeighing(recording, transition.at(), actorUserId);
        transitions.publish(transition, ShipmentDispatched.TYPE, ShipmentDispatched.of(recording));
    }

    private void insertWeighing(DispatchRecording recording, Timestamp at, UUID actor) {
        TareBasis basis = recording.weighing().tareBasis();
        jdbc.sql(INSERT_WEIGHING)
                .param("id", transitions.nextId())
                .param("shipmentId", recording.shipmentId())
                .param("grossKg", recording.weighing().grossKg())
                .param("tareKg", recording.weighing().tareKg())
                .param("bagCount", recording.weighing().bagCount())
                .param("ticketNo", recording.scaleTicketNo())
                .param("at", at)
                .param("actor", actor)
                .param("expectedTare", basis == null ? null : basis.expectedKg(), Types.NUMERIC)
                .param("tolerance", basis == null ? null : basis.tolerancePct(), Types.NUMERIC)
                .update();
    }
}
