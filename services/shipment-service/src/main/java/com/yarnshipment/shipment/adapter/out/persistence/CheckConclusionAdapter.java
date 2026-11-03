package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.CheckConclusionPort;
import com.yarnshipment.shipment.domain.check.CheckConclusion;
import com.yarnshipment.shipment.domain.check.ShipmentCompleted;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link CheckConclusionPort}'un JDBC uygulaması; geçiş sistem hesabıyla yazılır. Tamamlanan sevkiyat kilitlenir: bağlı
 * kayıtlar artık yalnızca Admin'in gerekçeli düzeltmesiyle değişir.
 */
@Component
final class CheckConclusionAdapter implements CheckConclusionPort {

    private static final String LOCK = "UPDATE shipment SET locked_at = ? WHERE id = ? AND locked_at IS NULL";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    CheckConclusionAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public void recordConclusion(CheckConclusion conclusion) {
        Transition transition = new Transition(
                conclusion.shipmentId(),
                conclusion.fromStatus(),
                conclusion.toStatus(),
                CheckConclusion.SYSTEM_ACTOR,
                Timestamp.from(conclusion.concludedAt()));
        transitions.actAs(CheckConclusion.SYSTEM_ACTOR);
        transitions.changeStatus(transition);
        if (conclusion.completed()) {
            jdbc.sql(LOCK).params(transition.at(), conclusion.shipmentId()).update();
            transitions.publish(transition, ShipmentCompleted.TYPE, ShipmentCompleted.of(conclusion));
        }
    }
}
