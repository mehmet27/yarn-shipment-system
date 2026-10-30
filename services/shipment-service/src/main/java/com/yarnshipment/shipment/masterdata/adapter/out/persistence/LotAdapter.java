package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.LotPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link LotPort}'un JDBC uygulaması. Okuma, partiyi lokasyon, iplik ve kalite adlarıyla birlikte getirir. Güncelleme
 * ve kilit yalnızca kilitsiz satırda yapılır; kilitli satırı ayrıca veritabanı trigger'ı korur (YS004).
 */
@Component
final class LotAdapter implements LotPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String LOTS = """
            FROM (SELECT l.id, l.site_id, l.lot_no, l.product_id, l.quality_grade_id, l.production_date,
                         l.quality_locked_at, l.active, s.name AS site_name, p.code AS product_code,
                         p.name AS product_name, g.code AS quality_grade_code, g.name AS quality_grade_name
                  FROM md_production_lot l
                  JOIN md_site s ON s.id = l.site_id
                  JOIN md_product p ON p.id = l.product_id
                  JOIN md_quality_grade g ON g.id = l.quality_grade_id) AS lot""";
    private static final String COLUMNS = "id, site_id, lot_no, product_id, quality_grade_id, production_date,"
            + " quality_locked_at, active, site_name, product_code, product_name, quality_grade_code,"
            + " quality_grade_name";
    private static final String LIST = LOTS + " WHERE (active OR :includeInactive)"
            + " AND (CAST(:pattern AS text) IS NULL OR lot_no ILIKE :pattern ESCAPE '\\'"
            + " OR product_code ILIKE :pattern ESCAPE '\\' OR product_name ILIKE :pattern ESCAPE '\\')"
            + " AND (:allSites OR site_id IN (:siteIds))";
    private static final String FIND = "SELECT " + COLUMNS + " " + LOTS + " WHERE id = :id";
    private static final String INSERT = """
            INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id, production_date,
                                           created_at, created_by)
            VALUES (:id, :siteId, :lotNo, :productId, :qualityGradeId, :productionDate, :at, :actor)""";
    private static final String UPDATE = """
            UPDATE md_production_lot
            SET lot_no = :lotNo, product_id = :productId, quality_grade_id = :qualityGradeId,
                production_date = :productionDate
            WHERE id = :id AND quality_locked_at IS NULL""";
    private static final String STATUS = "UPDATE md_production_lot SET active = :active WHERE id = :id";
    private static final String LOCK = """
            UPDATE md_production_lot SET quality_locked_at = :at, quality_locked_by = :actor
            WHERE id = :id AND quality_locked_at IS NULL""";
    private static final String ACTIVE_SITE = "SELECT EXISTS (SELECT 1 FROM md_site WHERE id = :id AND active)";
    private static final String ACTIVE_PRODUCT = "SELECT EXISTS (SELECT 1 FROM md_product WHERE id = :id AND active)";
    private static final String ACTIVE_QUALITY_GRADE =
            "SELECT EXISTS (SELECT 1 FROM md_quality_grade WHERE id = :id AND active)";

    private final JdbcClient jdbc;
    private final PagedQuery paged;
    private final Clock clock;

    LotAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.paged = new PagedQuery(jdbc);
        this.clock = clock;
    }

    @Override
    public Optional<ProductionLot> findLot(UUID id) {
        return jdbc.sql(FIND)
                .param("id", id)
                .query((rs, rowNumber) -> LotRows.lot(rs))
                .optional();
    }

    @Override
    public MasterDataPage<ProductionLot> findLots(AccessScope scope, MasterDataQuery query) {
        if (!scope.allSites() && scope.siteIds().isEmpty()) {
            return paged.empty(query);
        }
        List<UUID> siteIds = scope.siteIds().isEmpty() ? PagedQuery.NO_SITES : List.copyOf(scope.siteIds());
        return paged.page(
                new PagedQuery.Select(LIST, "production_date DESC, lot_no", COLUMNS),
                query,
                new PagedQuery.Filters(null, scope.allSites(), siteIds),
                LotRows::lot);
    }

    @Override
    public UUID insertLot(UUID actorId, UUID siteId, LotDetails details) {
        actAs(actorId);
        UUID id = UUID.randomUUID();
        try {
            bind(jdbc.sql(INSERT), details)
                    .param("id", id)
                    .param("siteId", siteId)
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", actorId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateCodeException("lotNo", details.lotNo());
        }
        return id;
    }

    @Override
    public boolean updateLot(UUID actorId, UUID id, LotDetails details) {
        actAs(actorId);
        try {
            return bind(jdbc.sql(UPDATE), details).param("id", id).update() > 0;
        } catch (DuplicateKeyException e) {
            throw new DuplicateCodeException("lotNo", details.lotNo());
        }
    }

    @Override
    public boolean setLotStatus(UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(STATUS)
                        .param("active", status.isActive())
                        .param("id", id)
                        .update()
                > 0;
    }

    @Override
    public boolean lockQuality(UUID actorId, UUID id, Instant lockedAt) {
        actAs(actorId);
        return jdbc.sql(LOCK)
                        .param("at", Timestamp.from(lockedAt))
                        .param("actor", actorId)
                        .param("id", id)
                        .update()
                > 0;
    }

    @Override
    public boolean activeSiteExists(UUID id) {
        return exists(ACTIVE_SITE, id);
    }

    @Override
    public boolean activeProductExists(UUID id) {
        return exists(ACTIVE_PRODUCT, id);
    }

    @Override
    public boolean activeQualityGradeExists(UUID id) {
        return exists(ACTIVE_QUALITY_GRADE, id);
    }

    private boolean exists(String sql, UUID id) {
        return Boolean.TRUE.equals(
                jdbc.sql(sql).param("id", id).query(Boolean.class).single());
    }

    private void actAs(UUID actorId) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, LotDetails details) {
        return spec.param("lotNo", details.lotNo())
                .param("productId", details.productId())
                .param("qualityGradeId", details.qualityGradeId())
                .param("productionDate", details.productionDate());
    }
}
