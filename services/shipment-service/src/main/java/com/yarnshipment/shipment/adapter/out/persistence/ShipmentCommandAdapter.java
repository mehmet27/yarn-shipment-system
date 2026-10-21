package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.domain.model.PickupConfirmation;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentCommandPort}'un JDBC uygulaması: şoför beyanları. */
@Component
final class ShipmentCommandAdapter implements ShipmentCommandPort {

    private static final String PICKUP = "PICKUP";
    private static final String COUNT_DECLARATIONS =
            "SELECT count(*) FROM driver_declaration WHERE shipment_id = ? AND declaration_type = ?";
    private static final String INSERT_DECLARATION = """
            INSERT INTO driver_declaration (id, shipment_id, declaration_type, driver_id, bag_count, seal_no,
                                            seal_intact, declared_at, created_at, created_by)
            VALUES (:id, :shipmentId, 'PICKUP', :driverId, :bagCount, :sealNo, true, :at, :at, :actor)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    ShipmentCommandAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public boolean hasPickupConfirmation(UUID shipmentId) {
        long count = jdbc.sql(COUNT_DECLARATIONS)
                .params(shipmentId, PICKUP)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public void recordPickup(PickupConfirmation confirmation, UUID actorUserId) {
        Transition transition = new Transition(
                confirmation.shipmentId(),
                confirmation.fromStatus(),
                confirmation.toStatus(),
                actorUserId,
                Timestamp.from(confirmation.declaredAt()));
        transitions.actAs(actorUserId);
        transitions.changeStatus(transition);
        jdbc.sql(INSERT_DECLARATION)
                .param("id", transitions.nextId())
                .param("shipmentId", confirmation.shipmentId())
                .param("driverId", confirmation.driverId())
                .param("bagCount", confirmation.bagCount())
                .param("sealNo", confirmation.sealNo())
                .param("at", transition.at())
                .param("actor", actorUserId)
                .update();
        transitions.publish(transition, ShipmentDispatched.TYPE, ShipmentDispatched.of(confirmation));
    }
}
