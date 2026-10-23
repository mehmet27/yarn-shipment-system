package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Durum geçişlerinin ortak yazımı: işlemi yapanı audit tetikleyicisine bildirir, durumu beklenen eski durum
 * koşuluyla değiştirir (CONC-07), geçmiş satırını (DOM-14) ve outbox olayını (EVT-03) yazar.
 */
@Component
final class ShipmentTransitionWriter {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String UPDATE_STATUS = """
            UPDATE shipment SET status = ?, updated_at = ?, updated_by = ?, version = version + 1
            WHERE id = ? AND status = ?""";
    private static final String INSERT_HISTORY = """
            INSERT INTO shipment_status_history (id, created_at, shipment_id, from_status, to_status, changed_by,
                                                 changed_at, reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
    private static final String INSERT_OUTBOX = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    ShipmentTransitionWriter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    UUID nextId() {
        return ids.next();
    }

    /** Transaction'daki tetikleyicilerin ({@code audit_local}) işlemi yapanı görmesi için. */
    void actAs(UUID actorUserId) {
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
    }

    void changeStatus(Transition transition) {
        String from = transition.from().name();
        String to = transition.to().name();
        Timestamp at = transition.at();
        int updated = jdbc.sql(UPDATE_STATUS)
                .params(to, at, transition.actorUserId(), transition.shipmentId(), from)
                .update();
        if (updated == 0) {
            throw new InvalidShipmentStatusException(transition.shipmentId(), transition.from(), transition.to());
        }
        appendHistory(transition.shipmentId(), from, transition);
    }

    /** İlk durum satırı: yeni sevkiyatın geçmişi önceki durum olmadan başlar. */
    void appendInitialHistory(Transition creation) {
        appendHistory(creation.shipmentId(), null, creation);
    }

    private void appendHistory(UUID shipmentId, @Nullable String from, Transition transition) {
        Timestamp at = transition.at();
        jdbc.sql(INSERT_HISTORY)
                .params(Arrays.asList(
                        ids.next(),
                        at,
                        shipmentId,
                        from,
                        transition.to().name(),
                        transition.actorUserId(),
                        at,
                        transition.reason()))
                .update();
    }

    void publish(Transition transition, String eventType, Object payload) {
        Timestamp at = transition.at();
        jdbc.sql(INSERT_OUTBOX)
                .params(
                        ids.next(),
                        at,
                        AGGREGATE_TYPE,
                        transition.shipmentId(),
                        eventType,
                        jsonMapper.writeValueAsString(payload),
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", transition.actorUserId().toString())),
                        at)
                .update();
    }

    /**
     * Bir durum geçişi.
     *
     * @param shipmentId sevkiyat
     * @param from beklenen eski durum
     * @param to yeni durum
     * @param actorUserId işlemi yapan
     * @param at geçiş zamanı
     * @param reason geçmişe yazılan gerekçe; yalnızca iptalde dolu
     */
    record Transition(
            UUID shipmentId,
            ShipmentStatus from,
            ShipmentStatus to,
            UUID actorUserId,
            Timestamp at,
            @Nullable String reason) {

        Transition(UUID shipmentId, ShipmentStatus from, ShipmentStatus to, UUID actorUserId, Timestamp at) {
            this(shipmentId, from, to, actorUserId, at, null);
        }
    }
}
