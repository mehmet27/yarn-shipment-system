package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.domain.arrival.ArrivalDeclaration;
import com.yarnshipment.shipment.domain.arrival.ShipmentArrived;
import com.yarnshipment.shipment.domain.model.PickupConfirmation;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import java.sql.Timestamp;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentCommandPort}'un JDBC uygulaması: şoför beyanları (teslim alma ve varış). */
@Component
final class ShipmentCommandAdapter implements ShipmentCommandPort {

    private static final String PICKUP = "PICKUP";
    private static final String ARRIVAL = "ARRIVAL";
    private static final String COUNT_DECLARATIONS =
            "SELECT count(*) FROM driver_declaration WHERE shipment_id = ? AND declaration_type = ?";
    private static final String INSERT_DECLARATION = """
            INSERT INTO driver_declaration (id, shipment_id, declaration_type, driver_id, bag_count, seal_no,
                                            seal_intact, declared_at, created_at, created_by)
            VALUES (:id, :shipmentId, :type, :driverId, :bagCount, :sealNo, :sealIntact, :at, :at, :actor)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    ShipmentCommandAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public boolean hasPickupConfirmation(UUID shipmentId) {
        return hasDeclaration(shipmentId, PICKUP);
    }

    @Override
    public boolean hasArrivalDeclaration(UUID shipmentId) {
        return hasDeclaration(shipmentId, ARRIVAL);
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
        insertDeclaration(
                transition,
                new Declaration(PICKUP, confirmation.driverId(), confirmation.bagCount(), confirmation.sealNo(), true));
        transitions.publish(transition, ShipmentDispatched.TYPE, ShipmentDispatched.of(confirmation));
    }

    @Override
    public void recordArrival(ArrivalDeclaration declaration, UUID actorUserId) {
        Transition transition = new Transition(
                declaration.shipmentId(),
                declaration.fromStatus(),
                declaration.toStatus(),
                actorUserId,
                Timestamp.from(declaration.declaredAt()));
        transitions.actAs(actorUserId);
        transitions.changeStatus(transition);
        insertDeclaration(
                transition,
                new Declaration(
                        ARRIVAL,
                        declaration.driverId(),
                        declaration.bagCount(),
                        declaration.sealNo(),
                        declaration.sealIntact()));
        transitions.publish(transition, ShipmentArrived.TYPE, ShipmentArrived.of(declaration));
    }

    private boolean hasDeclaration(UUID shipmentId, String type) {
        long count = jdbc.sql(COUNT_DECLARATIONS)
                .params(shipmentId, type)
                .query(Long.class)
                .single();
        return count > 0;
    }

    private void insertDeclaration(Transition transition, Declaration declaration) {
        jdbc.sql(INSERT_DECLARATION)
                .param("id", transitions.nextId())
                .param("shipmentId", transition.shipmentId())
                .param("type", declaration.type())
                .param("driverId", declaration.driverId())
                .param("bagCount", declaration.bagCount())
                .param("sealNo", declaration.sealNo())
                .param("sealIntact", declaration.sealIntact())
                .param("at", transition.at())
                .param("actor", transition.actorUserId())
                .update();
    }

    /** {@code driver_declaration} satırının beyana özgü alanları. */
    private record Declaration(
            String type,
            UUID driverId,
            int bagCount,
            @Nullable String sealNo,
            boolean sealIntact) {}
}
