package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.YarnLotPort;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.stock.LotRequest;
import com.yarnshipment.shipment.domain.stock.ResolvedLot;
import com.yarnshipment.shipment.domain.stock.YarnLotConflictException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Girişte yazılan parti numarasını {@code md_production_lot} ile eşler; yoksa kilitsiz parti açar. */
@Component
final class YarnLotAdapter implements YarnLotPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String FIND_LOT = """
            SELECT id, lot_no, product_id, yarn_supplier_id FROM md_production_lot
            WHERE upper(lot_no) = :lotNo AND site_id IN (:siteIds)
            ORDER BY active DESC, created_at LIMIT 1""";
    private static final String ACTIVE_GRADE =
            "SELECT EXISTS (SELECT 1 FROM md_quality_grade WHERE id = :id AND active)";
    private static final String INSERT_LOT = """
            INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id, production_date,
                                           yarn_supplier_id, created_at, created_by)
            VALUES (:id, :siteId, :lotNo, :productId, :gradeId, :today, :supplierId, :at, :actor)""";

    private final JdbcClient jdbc;
    private final Clock clock;

    YarnLotAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public ResolvedLot resolve(LotRequest request) {
        if (request.siteIds().isEmpty()) {
            throw new YarnLotConflictException(YarnLotConflictException.NO_SITE);
        }
        Optional<ExistingLot> existing = jdbc.sql(FIND_LOT)
                .param("lotNo", request.lotNo())
                .param("siteIds", List.copyOf(request.siteIds()))
                .query((rs, i) -> new ExistingLot(
                        rs.getObject("id", UUID.class),
                        rs.getString("lot_no"),
                        rs.getObject("product_id", UUID.class),
                        rs.getObject("yarn_supplier_id", UUID.class)))
                .optional();
        return existing.map(lot -> reuse(lot, request)).orElseGet(() -> open(request));
    }

    private static ResolvedLot reuse(ExistingLot lot, LotRequest request) {
        if (!lot.productId().equals(request.productId())) {
            throw new YarnLotConflictException(YarnLotConflictException.OTHER_YARN);
        }
        if (lot.supplierId() != null && !lot.supplierId().equals(request.supplierId())) {
            throw new YarnLotConflictException(YarnLotConflictException.OTHER_SUPPLIER);
        }
        return new ResolvedLot(lot.id(), lot.lotNo());
    }

    private ResolvedLot open(LotRequest request) {
        UUID gradeId = request.qualityGradeId();
        if (gradeId == null) {
            throw new YarnLotConflictException(YarnLotConflictException.GRADE_REQUIRED);
        }
        if (!Boolean.TRUE.equals(
                jdbc.sql(ACTIVE_GRADE).param("id", gradeId).query(Boolean.class).single())) {
            throw new InvalidLedgerInputException("qualityGradeId");
        }
        UUID id = UUID.randomUUID();
        UUID siteId = request.siteIds().stream().sorted().findFirst().orElseThrow();
        jdbc.sql(SET_ACTOR).param(request.actorId().toString()).query().singleValue();
        jdbc.sql(INSERT_LOT)
                .param("id", id)
                .param("siteId", siteId)
                .param("lotNo", request.lotNo())
                .param("productId", request.productId())
                .param("gradeId", gradeId)
                .param("today", LocalDate.now(clock))
                .param("supplierId", request.supplierId())
                .param("at", Timestamp.from(clock.instant()))
                .param("actor", request.actorId())
                .update();
        return new ResolvedLot(id, request.lotNo());
    }

    private record ExistingLot(
            UUID id, String lotNo, UUID productId, @Nullable UUID supplierId) {}
}
