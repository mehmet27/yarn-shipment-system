package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.BagReservationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagAlreadyReservedException;
import com.yarnshipment.bag.domain.model.BagNotAvailableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link BagReservationPort}'un JDBC uygulaması. Çuvallar kimliğe göre sıralı kilitlenir, böylece iki istek aynı
 * çuvalları ters sırayla kilitleyip kilitlenmez. Kısmi unique indeks ({@code bag_reservation_active_uq}) son
 * güvencedir.
 */
@Component
final class BagReservationAdapter implements BagReservationPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String LOCK = """
            SELECT b.id, b.unique_key, b.status, r.shipment_id
            FROM bag b
            LEFT JOIN bag_reservation r ON r.bag_id = b.id AND r.released_at IS NULL
            WHERE b.id IN (:ids)
            ORDER BY b.id
            FOR UPDATE OF b
            """;
    private static final String MARK = """
            UPDATE bag SET status = :status, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :id
            """;
    private static final String INSERT = """
            INSERT INTO bag_reservation (id, bag_id, shipment_id, reserved_at, reserved_by, created_at, created_by)
            VALUES (:id, :bagId, :shipmentId, :at, :actor, :at, :actor)
            """;
    private static final String RELEASE = """
            UPDATE bag_reservation
            SET released_at = :at, released_by = :actor, release_reason = :reason,
                updated_at = :at, updated_by = :actor, version = version + 1
            WHERE shipment_id = :shipmentId AND released_at IS NULL
            RETURNING bag_id
            """;

    private static final String RELEASE_SOME =
            RELEASE.replace("RETURNING", "AND bag_id IN (:bagIds)\n            RETURNING");

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;

    BagReservationAdapter(JdbcClient jdbc, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public void reserve(UUID shipmentId, List<Bag> bags, UUID actorUserId, Instant at) {
        if (bags.isEmpty()) {
            return;
        }
        actAs(actorUserId);
        Timestamp now = Timestamp.from(at);
        List<UUID> toReserve = new ArrayList<>();
        for (Locked bag : lock(bags)) {
            if (shipmentId.equals(bag.shipmentId())) {
                continue;
            }
            if (bag.shipmentId() != null) {
                throw new BagAlreadyReservedException(bag.uniqueKey());
            }
            if (bag.status() != BagStatus.CREATED) {
                throw new BagNotAvailableException(bag.uniqueKey(), bag.status());
            }
            toReserve.add(bag.id());
        }
        for (UUID bagId : toReserve) {
            mark(bagId, BagStatus.RESERVED, now, actorUserId);
            jdbc.sql(INSERT)
                    .param("id", ids.next())
                    .param("bagId", bagId)
                    .param("shipmentId", shipmentId)
                    .param("at", now)
                    .param("actor", actorUserId)
                    .update();
        }
    }

    @Override
    public int release(UUID shipmentId, String reason, UUID actorUserId, Instant at) {
        actAs(actorUserId);
        Timestamp now = Timestamp.from(at);
        List<UUID> released = jdbc.sql(RELEASE)
                .param("at", now)
                .param("actor", actorUserId)
                .param("reason", reason)
                .param("shipmentId", shipmentId)
                .query((rs, rowNumber) -> rs.getObject("bag_id", UUID.class))
                .list();
        for (UUID bagId : released) {
            mark(bagId, BagStatus.CREATED, now, actorUserId);
        }
        return released.size();
    }

    @Override
    public int releaseBags(SelectedRelease selected, UUID actorUserId, Instant at) {
        if (selected.bagIds().isEmpty()) {
            return 0;
        }
        actAs(actorUserId);
        Timestamp now = Timestamp.from(at);
        List<UUID> released = jdbc.sql(RELEASE_SOME)
                .param("at", now)
                .param("actor", actorUserId)
                .param("reason", selected.reason())
                .param("shipmentId", selected.shipmentId())
                .param("bagIds", selected.bagIds())
                .query((rs, rowNumber) -> rs.getObject("bag_id", UUID.class))
                .list();
        for (UUID bagId : released) {
            mark(bagId, BagStatus.CREATED, now, actorUserId);
        }
        return released.size();
    }

    private List<Locked> lock(List<Bag> bags) {
        return jdbc.sql(LOCK)
                .param("ids", bags.stream().map(Bag::id).toList())
                .query((rs, rowNumber) -> new Locked(
                        rs.getObject("id", UUID.class),
                        rs.getString("unique_key"),
                        BagStatus.valueOf(rs.getString("status")),
                        rs.getObject("shipment_id", UUID.class)))
                .list();
    }

    private void mark(UUID bagId, BagStatus status, Timestamp at, UUID actorUserId) {
        jdbc.sql(MARK)
                .param("status", status.name())
                .param("at", at)
                .param("actor", actorUserId)
                .param("id", bagId)
                .update();
    }

    /** Transaction'daki tetikleyicilerin ({@code audit_local}) işlemi yapanı görmesi için. */
    private void actAs(UUID actorUserId) {
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
    }

    /** Kilitlenmiş çuvalın o anki durumu ve açık rezervasyonu. */
    private record Locked(
            UUID id,
            String uniqueKey,
            BagStatus status,
            @Nullable UUID shipmentId) {}
}
