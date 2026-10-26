package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.CheckConclusionPort;
import com.yarnshipment.shipment.domain.check.CheckConclusion;
import com.yarnshipment.shipment.domain.check.ShipmentCompleted;
import java.sql.Timestamp;
import org.springframework.stereotype.Component;

/** {@link CheckConclusionPort}'un JDBC uygulaması; geçiş sistem hesabıyla yazılır. */
@Component
final class CheckConclusionAdapter implements CheckConclusionPort {

    private final ShipmentTransitionWriter transitions;

    CheckConclusionAdapter(ShipmentTransitionWriter transitions) {
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
            transitions.publish(transition, ShipmentCompleted.TYPE, ShipmentCompleted.of(conclusion));
        }
    }
}
