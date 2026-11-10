package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.YarnSupplierPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link YarnSupplierPort}'un JDBC uygulaması. Ad tekilliği büyük/küçük harf duyarsız benzersiz indeksle korunur; iz
 * {@code audit_local} trigger'ından düşer.
 */
@Component
final class YarnSupplierAdapter implements YarnSupplierPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String COLUMNS = "id, name, active";
    private static final String LIST =
            "FROM md_yarn_supplier WHERE (active OR :includeInactive)"
                    + " AND (CAST(:pattern AS text) IS NULL OR name ILIKE :pattern ESCAPE '\\')";
    private static final String INSERT = """
            INSERT INTO md_yarn_supplier (id, name, created_at, created_by)
            VALUES (:id, :name, :at, :actor)""";
    private static final String UPDATE = "UPDATE md_yarn_supplier SET name = :name WHERE id = :id RETURNING " + COLUMNS;
    private static final String STATUS = "UPDATE md_yarn_supplier SET active = :active WHERE id = :id";

    private final JdbcClient jdbc;
    private final PagedQuery paged;
    private final Clock clock;

    YarnSupplierAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.paged = new PagedQuery(jdbc);
        this.clock = clock;
    }

    @Override
    public MasterDataPage<YarnSupplier> findYarnSuppliers(MasterDataQuery query) {
        return paged.page(
                new PagedQuery.Select(LIST, "lower(name)", COLUMNS),
                query,
                new PagedQuery.Filters(null, true, PagedQuery.NO_SITES),
                YarnSupplierAdapter::supplier);
    }

    @Override
    public YarnSupplier insertYarnSupplier(UUID actorId, String name) {
        actAs(actorId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql(INSERT)
                    .param("id", id)
                    .param("name", name)
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", actorId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateCodeException("name", name);
        }
        return new YarnSupplier(id, name, true);
    }

    @Override
    public Optional<YarnSupplier> updateYarnSupplier(UUID actorId, UUID id, String name) {
        actAs(actorId);
        try {
            return jdbc.sql(UPDATE)
                    .param("id", id)
                    .param("name", name)
                    .query((rs, rowNumber) -> supplier(rs))
                    .optional();
        } catch (DuplicateKeyException e) {
            throw new DuplicateCodeException("name", name);
        }
    }

    @Override
    public boolean setYarnSupplierStatus(UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(STATUS)
                        .param("active", status.isActive())
                        .param("id", id)
                        .update()
                > 0;
    }

    private void actAs(UUID actorId) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
    }

    private static YarnSupplier supplier(ResultSet rs) throws SQLException {
        return new YarnSupplier(rs.getObject("id", UUID.class), rs.getString("name"), rs.getBoolean("active"));
    }
}
