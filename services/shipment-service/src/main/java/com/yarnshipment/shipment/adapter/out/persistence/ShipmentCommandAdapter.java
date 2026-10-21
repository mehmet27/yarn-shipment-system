package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentCommandPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.PickupConfirmation;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link ShipmentCommandPort}'un JDBC uygulaması. Durum güncellemesi beklenen eski durumu WHERE koşulunda arar;
 * eşzamanlı bir geçiş satırı değiştirdiyse güncelleme boşa düşer ve 409'a çevrilir (CONC-07).
 */
@Component
final class ShipmentCommandAdapter implements ShipmentCommandPort {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String PICKUP = "PICKUP";
    private static final String COUNT_DECLARATIONS =
            "SELECT count(*) FROM driver_declaration WHERE shipment_id = ? AND declaration_type = ?";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String UPDATE_STATUS = """
            UPDATE shipment SET status = ?, updated_at = ?, updated_by = ?, version = version + 1
            WHERE id = ? AND status = ?""";
    private static final String INSERT_DECLARATION = """
            INSERT INTO driver_declaration (id, shipment_id, declaration_type, driver_id, bag_count, seal_no,
                                            seal_intact, declared_at, created_at, created_by)
            VALUES (:id, :shipmentId, 'PICKUP', :driverId, :bagCount, :sealNo, true, :at, :at, :actor)""";
    private static final String INSERT_HISTORY = """
            INSERT INTO shipment_status_history (id, created_at, shipment_id, from_status, to_status, changed_by,
                                                 changed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";
    private static final String INSERT_OUTBOX = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    ShipmentCommandAdapter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
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
        Timestamp at = Timestamp.from(confirmation.declaredAt());
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
        changeStatus(confirmation, actorUserId, at);
        jdbc.sql(INSERT_DECLARATION)
                .param("id", ids.next())
                .param("shipmentId", confirmation.shipmentId())
                .param("driverId", confirmation.driverId())
                .param("bagCount", confirmation.bagCount())
                .param("sealNo", confirmation.sealNo())
                .param("at", at)
                .param("actor", actorUserId)
                .update();
        insertOutbox(ShipmentDispatched.of(confirmation), actorUserId, at);
    }

    /** DOM-14: her geçiş geçmiş tablosuna da yazılır. */
    private void changeStatus(PickupConfirmation confirmation, UUID actorUserId, Timestamp at) {
        String from = confirmation.fromStatus().name();
        String to = confirmation.toStatus().name();
        int updated = jdbc.sql(UPDATE_STATUS)
                .params(to, at, actorUserId, confirmation.shipmentId(), from)
                .update();
        if (updated == 0) {
            throw new InvalidShipmentStatusException(
                    confirmation.shipmentId(), confirmation.fromStatus(), confirmation.toStatus());
        }
        jdbc.sql(INSERT_HISTORY)
                .params(ids.next(), at, confirmation.shipmentId(), from, to, actorUserId, at)
                .update();
    }

    private void insertOutbox(ShipmentDispatched event, UUID actorUserId, Timestamp at) {
        jdbc.sql(INSERT_OUTBOX)
                .params(
                        ids.next(),
                        at,
                        AGGREGATE_TYPE,
                        event.shipmentId(),
                        ShipmentDispatched.TYPE,
                        jsonMapper.writeValueAsString(event),
                        jsonMapper.writeValueAsString(Map.of("actorId", actorUserId.toString())),
                        at)
                .update();
    }
}
