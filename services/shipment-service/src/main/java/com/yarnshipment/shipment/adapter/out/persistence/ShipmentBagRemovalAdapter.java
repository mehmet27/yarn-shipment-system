package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentBagRemovalPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentBagRemovalPort}'un JDBC uygulaması. Satır silinmez; çıkarma zamanı ve kişisi işaretlenir. */
@Component
final class ShipmentBagRemovalAdapter implements ShipmentBagRemovalPort {

    private static final String REMOVE = """
            UPDATE shipment_bag
            SET removed_at = :at, removed_by = :actor, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE shipment_id = :shipmentId AND unique_key = :uniqueKey AND removed_at IS NULL""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    ShipmentBagRemovalAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public int remove(UUID shipmentId, String uniqueKey, UUID actorUserId, Instant at) {
        transitions.actAs(actorUserId);
        Timestamp now = Timestamp.from(at);
        return jdbc.sql(REMOVE)
                .param("at", now)
                .param("actor", actorUserId)
                .param("shipmentId", shipmentId)
                .param("uniqueKey", uniqueKey)
                .update();
    }
}
