package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.DuplicateSubcontractorException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link MasterDataCommandPort}'un JDBC uygulaması. Değişikliği yapan kullanıcı {@code app.actor_id} ile işlenir,
 * iz {@code audit_local} trigger'ından düşer. Küçük bir tablo olduğu için kimlikler rastgele (v4) üretilir.
 */
@Component
final class MasterDataCommandAdapter implements MasterDataCommandPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String INSERT_SUBCONTRACTOR =
            "INSERT INTO md_subcontractor (id, title, created_at, created_by) VALUES (:id, :title, :at, :actor)";
    private static final String UPDATE_SUBCONTRACTOR = "UPDATE md_subcontractor SET title = :title WHERE id = :id";
    private static final String ACTIVATE_SUBCONTRACTOR = "UPDATE md_subcontractor SET active = :active WHERE id = :id";
    private static final String ACTIVE_SUBCONTRACTOR =
            "SELECT count(*) FROM md_subcontractor WHERE id = :id AND active";
    private static final String INSERT_DRIVER = """
            INSERT INTO md_driver (id, full_name, subcontractor_id, created_at, created_by)
            VALUES (:id, :name, :subcontractorId, :at, :actor)""";
    private static final String UPDATE_DRIVER =
            "UPDATE md_driver SET full_name = :name, subcontractor_id = :subcontractorId WHERE id = :id";
    private static final String ACTIVATE_DRIVER = "UPDATE md_driver SET active = :active WHERE id = :id";

    private final JdbcClient jdbc;
    private final Clock clock;

    MasterDataCommandAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Subcontractor insertSubcontractor(UUID actorId, String title) {
        actAs(actorId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql(INSERT_SUBCONTRACTOR)
                    .param("id", id)
                    .param("title", title)
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", actorId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateSubcontractorException(title);
        }
        return new Subcontractor(id, title);
    }

    @Override
    public boolean updateSubcontractor(UUID actorId, UUID id, String title) {
        actAs(actorId);
        try {
            return jdbc.sql(UPDATE_SUBCONTRACTOR)
                            .param("title", title)
                            .param("id", id)
                            .update()
                    > 0;
        } catch (DuplicateKeyException e) {
            throw new DuplicateSubcontractorException(title);
        }
    }

    @Override
    public boolean setSubcontractorStatus(UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(ACTIVATE_SUBCONTRACTOR)
                        .param("active", status.isActive())
                        .param("id", id)
                        .update()
                > 0;
    }

    @Override
    public boolean activeSubcontractorExists(UUID id) {
        return jdbc.sql(ACTIVE_SUBCONTRACTOR).param("id", id).query(Long.class).single() > 0;
    }

    @Override
    public Driver insertDriver(UUID actorId, DriverDetails details) {
        actAs(actorId);
        UUID id = UUID.randomUUID();
        jdbc.sql(INSERT_DRIVER)
                .param("id", id)
                .param("name", details.fullName())
                .param("subcontractorId", details.subcontractorId())
                .param("at", Timestamp.from(clock.instant()))
                .param("actor", actorId)
                .update();
        return new Driver(id, details.fullName(), details.subcontractorId());
    }

    @Override
    public boolean updateDriver(UUID actorId, UUID id, DriverDetails details) {
        actAs(actorId);
        return jdbc.sql(UPDATE_DRIVER)
                        .param("name", details.fullName())
                        .param("subcontractorId", details.subcontractorId())
                        .param("id", id)
                        .update()
                > 0;
    }

    @Override
    public boolean setDriverStatus(UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(ACTIVATE_DRIVER)
                        .param("active", status.isActive())
                        .param("id", id)
                        .update()
                > 0;
    }

    private void actAs(UUID actorId) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
    }
}
