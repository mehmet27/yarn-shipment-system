package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.VehicleSiteCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.DuplicatePlateException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link VehicleSiteCommandPort}'un JDBC uygulaması. Değişikliği yapan kullanıcı {@code app.actor_id} ile işlenir,
 * iz {@code audit_local} trigger'ından düşer. Plaka tekilliği veritabanı kısıtıyla korunur.
 */
@Component
final class VehicleSiteCommandAdapter implements VehicleSiteCommandPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String INSERT_VEHICLE =
            "INSERT INTO md_vehicle (id, plate_no, created_at, created_by) VALUES (:id, :value, :at, :actor)";
    private static final String UPDATE_VEHICLE = "UPDATE md_vehicle SET plate_no = :value WHERE id = :id";
    private static final String STATUS_VEHICLE = "UPDATE md_vehicle SET active = :active WHERE id = :id";
    private static final String INSERT_SITE =
            "INSERT INTO md_site (id, name, created_at, created_by) VALUES (:id, :value, :at, :actor)";
    private static final String UPDATE_SITE = "UPDATE md_site SET name = :value WHERE id = :id";
    private static final String STATUS_SITE = "UPDATE md_site SET active = :active WHERE id = :id";

    private final JdbcClient jdbc;
    private final Clock clock;

    VehicleSiteCommandAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Vehicle insertVehicle(UUID actorId, String plateNo) {
        UUID id = insert(INSERT_VEHICLE, actorId, plateNo);
        return new Vehicle(id, plateNo);
    }

    @Override
    public boolean updateVehicle(UUID actorId, UUID id, String plateNo) {
        actAs(actorId);
        try {
            return jdbc.sql(UPDATE_VEHICLE)
                            .param("value", plateNo)
                            .param("id", id)
                            .update()
                    > 0;
        } catch (DuplicateKeyException e) {
            throw new DuplicatePlateException(plateNo);
        }
    }

    @Override
    public boolean setVehicleStatus(UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(STATUS_VEHICLE)
                        .param("active", status.isActive())
                        .param("id", id)
                        .update()
                > 0;
    }

    @Override
    public Site insertSite(UUID actorId, String name) {
        UUID id = insert(INSERT_SITE, actorId, name);
        return new Site(id, name);
    }

    @Override
    public boolean updateSite(UUID actorId, UUID id, String name) {
        actAs(actorId);
        return jdbc.sql(UPDATE_SITE).param("value", name).param("id", id).update() > 0;
    }

    @Override
    public boolean setSiteStatus(UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(STATUS_SITE)
                        .param("active", status.isActive())
                        .param("id", id)
                        .update()
                > 0;
    }

    private UUID insert(String sql, UUID actorId, String value) {
        actAs(actorId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql(sql)
                    .param("id", id)
                    .param("value", value)
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", actorId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicatePlateException(value);
        }
        return id;
    }

    private void actAs(UUID actorId) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
    }
}
