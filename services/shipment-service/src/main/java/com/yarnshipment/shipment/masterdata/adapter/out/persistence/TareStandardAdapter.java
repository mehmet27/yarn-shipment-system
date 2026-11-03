package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.TareBasisPort;
import com.yarnshipment.shipment.domain.model.TareBasis;
import com.yarnshipment.shipment.masterdata.application.port.out.TareStandardPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.PackagingType;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandard;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandardDetails;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link TareStandardPort} ve {@link TareBasisPort}'un JDBC uygulaması. Aynı lokasyon ve ambalaj için ikinci kayıt
 * eklenmez, değerler güncellenir; iz {@code audit_local} trigger'ından düşer.
 */
@Component
final class TareStandardAdapter implements TareStandardPort, TareBasisPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String SELECT = """
            SELECT t.id, t.site_id, s.name AS site_name, t.packaging_type, t.standard_tare_kg, t.max_deviation_pct
            FROM md_tare_standard t JOIN md_site s ON s.id = t.site_id""";
    private static final String LIST =
            SELECT + " WHERE (CAST(:siteId AS uuid) IS NULL OR t.site_id = :siteId) ORDER BY s.name, t.packaging_type";
    private static final String UPSERT = """
            INSERT INTO md_tare_standard (id, site_id, packaging_type, standard_tare_kg, max_deviation_pct, created_at,
                                          created_by)
            VALUES (:id, :siteId, :type, :tare, :pct, :at, :actor)
            ON CONFLICT (site_id, packaging_type) DO UPDATE
                SET standard_tare_kg = EXCLUDED.standard_tare_kg, max_deviation_pct = EXCLUDED.max_deviation_pct,
                    updated_at = EXCLUDED.created_at, updated_by = EXCLUDED.created_by""";
    private static final String BAG_STANDARD = """
            SELECT standard_tare_kg, max_deviation_pct FROM md_tare_standard
            WHERE site_id = ? AND packaging_type = 'BAG'""";

    private final JdbcClient jdbc;
    private final Clock clock;

    TareStandardAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public List<TareStandard> findTareStandards(@Nullable UUID siteId) {
        return jdbc.sql(LIST)
                .param("siteId", siteId, java.sql.Types.OTHER)
                .query((rs, rowNumber) -> tareStandard(rs))
                .list();
    }

    @Override
    public TareStandard upsertTareStandard(
            UUID actorId, UUID siteId, PackagingType packagingType, TareStandardDetails details) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
        try {
            jdbc.sql(UPSERT)
                    .param("id", UUID.randomUUID())
                    .param("siteId", siteId)
                    .param("type", packagingType.name())
                    .param("tare", details.standardTareKg())
                    .param("pct", details.maxDeviationPct())
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", actorId)
                    .update();
        } catch (DataIntegrityViolationException e) {
            throw new InvalidMasterDataException("siteId", "unknown site");
        }
        return jdbc.sql(SELECT + " WHERE t.site_id = :siteId AND t.packaging_type = :type")
                .param("siteId", siteId)
                .param("type", packagingType.name())
                .query((rs, rowNumber) -> tareStandard(rs))
                .single();
    }

    @Override
    public @Nullable TareBasis findBagTareBasis(UUID siteId, int bagCount) {
        return jdbc.sql(BAG_STANDARD)
                .param(siteId)
                .query((rs, rowNumber) -> TareBasis.of(
                        rs.getBigDecimal("standard_tare_kg"), rs.getBigDecimal("max_deviation_pct"), bagCount))
                .optional()
                .orElse(null);
    }

    private static TareStandard tareStandard(ResultSet rs) throws SQLException {
        BigDecimal tare = rs.getBigDecimal("standard_tare_kg");
        BigDecimal pct = rs.getBigDecimal("max_deviation_pct");
        return new TareStandard(
                rs.getObject("id", UUID.class),
                rs.getObject("site_id", UUID.class),
                rs.getString("site_name"),
                PackagingType.valueOf(rs.getString("packaging_type")),
                new TareStandardDetails(tare, pct));
    }
}
