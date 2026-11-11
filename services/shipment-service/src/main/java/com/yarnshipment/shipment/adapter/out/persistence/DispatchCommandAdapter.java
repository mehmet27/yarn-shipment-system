package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.DispatchCommandPort;
import com.yarnshipment.shipment.domain.model.DispatchRecording;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.Weighing.TareBasis;
import com.yarnshipment.shipment.domain.model.WeightCheck;
import com.yarnshipment.shipment.domain.model.WeightMismatchDetected;
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

    private static final String SUPERSEDE_PREVIOUS = """
            UPDATE weighing SET superseded_by = :newId, updated_at = now(), version = version + 1
            WHERE shipment_id = :shipmentId AND side = 'DISPATCH' AND superseded_by IS NULL AND id <> :newId""";
    private static final String INSERT_WEIGHT_CHECK = """
            INSERT INTO weight_check (id, shipment_id, weighing_id, empty_kg, loaded_kg, expected_kg, diff_kg,
                                      tolerance_kg, shortfall_reason, created_at, created_by)
            VALUES (:id, :shipmentId, :weighingId, :emptyKg, :loadedKg, :expectedKg, :diffKg, :toleranceKg,
                    :shortfall, :at, :actor)""";

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
        UUID weighingId = insertWeighing(recording, transition.at(), actorUserId);
        WeightCheck mismatch = recording.mismatch();
        if (mismatch == null) {
            transitions.publish(transition, ShipmentDispatched.TYPE, ShipmentDispatched.of(recording));
            return;
        }
        insertWeightCheck(recording, mismatch, weighingId, transition);
        transitions.publish(transition, WeightMismatchDetected.TYPE, WeightMismatchDetected.of(recording, mismatch));
    }

    private void insertWeightCheck(
            DispatchRecording recording, WeightCheck check, UUID weighingId, Transition transition) {
        Timestamp at = transition.at();
        UUID actor = transition.actorUserId();
        jdbc.sql(INSERT_WEIGHT_CHECK)
                .param("id", transitions.nextId())
                .param("shipmentId", recording.shipmentId())
                .param("weighingId", weighingId)
                .param("emptyKg", check.emptyKg())
                .param("loadedKg", check.loadedKg())
                .param("expectedKg", check.expectedKg())
                .param("diffKg", check.diffKg())
                .param("toleranceKg", check.toleranceKg())
                .param("shortfall", check.shortfallReason())
                .param("at", at)
                .param("actor", actor)
                .update();
    }

    /** Yeni tartım eklenir, öncekinin yerine geçtiği işaretlenir (silinmez). */
    private UUID insertWeighing(DispatchRecording recording, Timestamp at, UUID actor) {
        TareBasis basis = recording.weighing().tareBasis();
        UUID id = transitions.nextId();
        jdbc.sql(INSERT_WEIGHING)
                .param("id", id)
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
        jdbc.sql(SUPERSEDE_PREVIOUS)
                .param("newId", id)
                .param("shipmentId", recording.shipmentId())
                .update();
        return id;
    }
}
