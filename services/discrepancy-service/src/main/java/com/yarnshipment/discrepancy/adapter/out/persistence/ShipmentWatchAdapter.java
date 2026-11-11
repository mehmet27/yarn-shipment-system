package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@code shipment_watch} tablosu. Olaylar tekrar gelebildiği için oluşturma çakışmada hiçbir şey yazmaz ve adımlar
 * yalnızca boş alanı doldurur: ilk gelen zaman kalır.
 */
@Component
final class ShipmentWatchAdapter implements ShipmentWatchPort {

    /** Olaydan yazılan kayıtların sahibi; shipment-service'teki sistem hesabıyla aynı. */
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private static final String INSERT = """
            INSERT INTO shipment_watch (shipment_id, shipment_no, site_id, subcontractor_id, shipment_created_at,
                                        created_at, created_by)
            VALUES (:shipmentId, :shipmentNo, :siteId, :subcontractorId, :createdAt, :createdAt, :actor)
            ON CONFLICT (shipment_id) DO NOTHING""";
    private static final String SET_DISPATCHED = """
            UPDATE shipment_watch
               SET dispatched_at = :at, updated_at = :at, updated_by = :actor, version = version + 1
             WHERE shipment_id = :shipmentId AND dispatched_at IS NULL""";
    private static final String SET_CLOSED = """
            UPDATE shipment_watch
               SET closed_at = :at, updated_at = :at, updated_by = :actor, version = version + 1
             WHERE shipment_id = :shipmentId AND closed_at IS NULL""";
    private static final String SET_TRANSIT_ALERTED = """
            UPDATE shipment_watch
               SET transit_alerted_at = :at, updated_at = :at, updated_by = :actor, version = version + 1
             WHERE shipment_id = :shipmentId AND transit_alerted_at IS NULL""";
    private static final String COLUMNS = "shipment_id, shipment_no, site_id, subcontractor_id, dispatched_at";
    private static final String FIND = "SELECT " + COLUMNS + " FROM shipment_watch WHERE shipment_id = :shipmentId";
    private static final String FIND_IN_TRANSIT = "SELECT " + COLUMNS + """
             FROM shipment_watch
            WHERE dispatched_at < :before AND closed_at IS NULL
              AND transit_alerted_at IS NULL
            ORDER BY dispatched_at
            LIMIT :limit""";

    private final JdbcClient jdbc;

    ShipmentWatchAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void recordCreated(WatchedShipment shipment, Instant createdAt) {
        jdbc.sql(INSERT)
                .param("shipmentId", shipment.shipmentId())
                .param("shipmentNo", shipment.shipmentNo())
                .param("siteId", shipment.siteId())
                .param("subcontractorId", shipment.subcontractorId())
                .param("createdAt", Timestamp.from(createdAt))
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    @Override
    public void recordDispatched(UUID shipmentId, Instant dispatchedAt) {
        mark(SET_DISPATCHED, shipmentId, dispatchedAt);
    }

    @Override
    public void recordClosed(UUID shipmentId, Instant closedAt) {
        mark(SET_CLOSED, shipmentId, closedAt);
    }

    @Override
    public Optional<WatchedShipment> findShipment(UUID shipmentId) {
        return jdbc.sql(FIND)
                .param("shipmentId", shipmentId)
                .query((rs, rowNum) -> shipment(rs))
                .optional();
    }

    @Override
    public List<WatchedShipment> findInTransitSince(Instant dispatchedBefore, int limit) {
        return jdbc.sql(FIND_IN_TRANSIT)
                .param("before", Timestamp.from(dispatchedBefore))
                .param("limit", limit)
                .query((rs, rowNum) -> shipment(rs))
                .list();
    }

    @Override
    public boolean markTransitAlerted(UUID shipmentId, Instant at) {
        return mark(SET_TRANSIT_ALERTED, shipmentId, at) > 0;
    }

    private int mark(String sql, UUID shipmentId, Instant at) {
        return jdbc.sql(sql)
                .param("shipmentId", shipmentId)
                .param("at", Timestamp.from(at))
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    private static WatchedShipment shipment(ResultSet rs) throws SQLException {
        Timestamp dispatchedAt = rs.getTimestamp("dispatched_at");
        return new WatchedShipment(
                rs.getObject("shipment_id", UUID.class),
                rs.getString("shipment_no"),
                rs.getObject("site_id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                dispatchedAt == null ? null : dispatchedAt.toInstant());
    }
}
