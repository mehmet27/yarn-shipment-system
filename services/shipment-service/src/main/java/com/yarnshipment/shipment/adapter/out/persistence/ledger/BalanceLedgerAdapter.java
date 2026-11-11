package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.application.port.out.ledger.DispatchLedgerPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.DispatchContext;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.DispatchLine;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.balance.MovementKind;
import com.yarnshipment.shipment.domain.balance.YarnRef;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Component;

/**
 * Bakiye defterinin JDBC uygulaması. Bakiye hareketlerin toplamıdır; yoldaki kilo (KR-12) henüz tamamlanmamış
 * sevkiyatların çıkış tartısı netidir. Reçete satırları tablosu ayrı bir değişiklikle gelir; yoksa ya da toplamı
 * tartıyla tutmuyorsa sevkiyat "(reçetesiz)" tek satır yazılır.
 */
@Component
final class BalanceLedgerAdapter implements BalanceQueryPort, DispatchLedgerPort {

    static final String UNSPECIFIED_YARN = "(reçetesiz)";

    private static final String BALANCES = """
            SELECT t.subcontractor_id, coalesce(m.title, t.subcontractor_id::text) AS title, t.yarn_type,
                   sum(t.balance_kg) AS balance_kg, sum(t.in_transit_kg) AS in_transit_kg
            FROM (
                SELECT subcontractor_id, yarn_type, net_kg AS balance_kg, 0::numeric AS in_transit_kg
                FROM balance_movement
                UNION ALL
                SELECT s.subcontractor_id, '%s', 0::numeric, w.net_kg
                FROM shipment s
                JOIN weighing w ON w.shipment_id = s.id AND w.side = 'DISPATCH' AND w.superseded_by IS NULL
                WHERE s.status = 'AWAITING_DELIVERY'
            ) t
            LEFT JOIN md_subcontractor m ON m.id = t.subcontractor_id
            WHERE (CAST(:subcontractorId AS uuid) IS NULL OR t.subcontractor_id = CAST(:subcontractorId AS uuid))
            GROUP BY t.subcontractor_id, m.title, t.yarn_type
            ORDER BY title, t.yarn_type""".formatted(UNSPECIFIED_YARN);
    private static final String DISPATCHED_BY_YARN = """
            SELECT yarn_type, sum(net_kg) AS net_kg FROM balance_movement
            WHERE shipment_id = :shipmentId AND kind = 'DISPATCH'
            GROUP BY yarn_type ORDER BY min(source_ref)""";
    private static final String WEIGHED_NET = """
            SELECT coalesce(sum(net_kg), 0) FROM weighing
            WHERE shipment_id = ? AND side = 'DISPATCH' AND superseded_by IS NULL""";
    private static final String SOURCE_SUBCONTRACTOR = """
            SELECT source_subcontractor_id FROM shipment
            WHERE id = :shipmentId AND source_subcontractor_id IS NOT NULL""";
    /** Merkezden çıkan sevkiyatın iplikleri merkez stoktan düşer; gönderen fasoncu varsa merkez stoğa dokunulmaz. */
    private static final String CENTRAL_STOCK_OUT = """
            INSERT INTO central_stock_movement (id, yarn_type, kind, net_kg, shipment_id, occurred_at, created_by,
                                                source_ref)
            SELECT gen_random_uuid(), coalesce(p.name, bm.yarn_type), 'TO_SUBCONTRACTOR', -bm.net_kg, bm.shipment_id,
                   bm.occurred_at, bm.created_by, 'CENTRAL_OUT:' || bm.source_ref
            FROM balance_movement bm
            JOIN shipment s ON s.id = bm.shipment_id AND s.source_subcontractor_id IS NULL
            LEFT JOIN md_product p ON p.id::text = bm.yarn_type
            WHERE bm.shipment_id = :shipmentId AND bm.kind = 'DISPATCH'
            ON CONFLICT (source_ref) DO NOTHING""";

    private static final String LINE_COLUMNS = """
            SELECT column_name FROM information_schema.columns
            WHERE table_schema = current_schema() AND table_name = 'shipment_line'""";
    private static final String MOVEMENT_COLUMNS = """
            SELECT id, subcontractor_id, yarn_type, lot_id, shipment_id, kind, net_kg, occurred_at, created_by, reason,
                   source_ref
            FROM balance_movement""";

    private final JdbcClient jdbc;
    private final BalanceMovementWriter writer;

    BalanceLedgerAdapter(JdbcClient jdbc, BalanceMovementWriter writer) {
        this.jdbc = jdbc;
        this.writer = writer;
    }

    @Override
    public List<BalanceRow> findBalances(@Nullable UUID subcontractorId) {
        return jdbc.sql(BALANCES)
                .param("subcontractorId", subcontractorId, Types.OTHER)
                .query((rs, i) -> new BalanceRow(
                        rs.getObject("subcontractor_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("yarn_type"),
                        rs.getBigDecimal("balance_kg"),
                        rs.getBigDecimal("in_transit_kg")))
                .list();
    }

    @Override
    public MovementPage findMovements(ListMovementsQuery query) {
        Map<String, Object> params = new LinkedHashMap<>();
        List<String> conditions = new ArrayList<>();
        if (query.subcontractorId() != null) {
            conditions.add("subcontractor_id = :subcontractorId");
            params.put("subcontractorId", query.subcontractorId());
        }
        if (query.yarnType() != null) {
            conditions.add("yarn_type = :yarnType");
            params.put("yarnType", query.yarnType());
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        Long total = bind(jdbc.sql("SELECT count(*) FROM balance_movement" + where), params)
                .query(Long.class)
                .single();
        List<BalanceMovement> items = bind(
                        jdbc.sql(MOVEMENT_COLUMNS + where
                                + " ORDER BY occurred_at DESC, id DESC LIMIT :limit OFFSET :offset"),
                        params)
                .param("limit", query.size())
                .param("offset", (long) query.page() * query.size())
                .query((rs, rowNumber) -> toMovement(rs))
                .list();
        return new MovementPage(items, query.page(), query.size(), total);
    }

    @Override
    public Map<String, BigDecimal> findDispatchedByYarn(UUID shipmentId) {
        Map<String, BigDecimal> byYarn = new LinkedHashMap<>();
        jdbc.sql(DISPATCHED_BY_YARN)
                .param("shipmentId", shipmentId)
                .query((rs, i) -> byYarn.put(rs.getString("yarn_type"), rs.getBigDecimal("net_kg")))
                .list();
        return byYarn;
    }

    @Override
    public BigDecimal recordDispatch(UUID shipmentId, UUID subcontractorId, Instant at, UUID createdBy) {
        BigDecimal weighed = Kilograms.round(
                jdbc.sql(WEIGHED_NET).param(shipmentId).query(BigDecimal.class).single());
        if (weighed.signum() <= 0) {
            return Kilograms.ZERO;
        }
        DispatchContext context = new DispatchContext(subcontractorId, shipmentId, at, createdBy);
        @Nullable
        UUID source = jdbc.sql(SOURCE_SUBCONTRACTOR)
                .param("shipmentId", shipmentId)
                .query(UUID.class)
                .optional()
                .orElse(null);
        for (DispatchLine line : dispatchLines(shipmentId, weighed)) {
            writer.append(BalanceMovement.dispatch(line, context));
            if (source != null) {
                writer.append(BalanceMovement.transferOut(source, line, context));
            }
        }
        jdbc.sql(CENTRAL_STOCK_OUT).param("shipmentId", shipmentId).update();
        return weighed;
    }

    /** Reçete satırları tartı netine eşitse satırlar, değilse tek "(reçetesiz)" satır. */
    private List<DispatchLine> dispatchLines(UUID shipmentId, BigDecimal weighed) {
        List<DispatchLine> lines = recipeLines(shipmentId);
        BigDecimal total = lines.stream().map(DispatchLine::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lines.isEmpty() || total.compareTo(weighed) != 0) {
            return List.of(new DispatchLine(1, UNSPECIFIED_YARN, null, weighed));
        }
        return lines;
    }

    private List<DispatchLine> recipeLines(UUID shipmentId) {
        Set<String> columns = Set.copyOf(
                jdbc.sql(LINE_COLUMNS).query((rs, i) -> rs.getString(1)).list());
        boolean hasText = columns.contains("yarn_type_text");
        boolean hasId = columns.contains("yarn_type_id");
        if (!columns.containsAll(Set.of("line_no", "net_kg")) || !(hasText || hasId)) {
            return List.of();
        }
        String yarn = hasText && hasId
                ? "coalesce(yarn_type_text, yarn_type_id::text)"
                : hasText ? "yarn_type_text" : "yarn_type_id::text";
        String lotColumn = columns.contains("lot_id") ? "lot_id" : "NULL::uuid";
        return jdbc.sql("SELECT line_no, " + yarn + " AS yarn_type, " + lotColumn
                        + " AS lot_id, net_kg FROM shipment_line WHERE shipment_id = :shipmentId AND net_kg > 0"
                        + " AND " + yarn + " IS NOT NULL ORDER BY line_no")
                .param("shipmentId", shipmentId)
                .query((rs, i) -> new DispatchLine(
                        rs.getInt("line_no"),
                        rs.getString("yarn_type"),
                        rs.getObject("lot_id", UUID.class),
                        Kilograms.round(rs.getBigDecimal("net_kg"))))
                .list();
    }

    private static StatementSpec bind(StatementSpec spec, Map<String, Object> params) {
        StatementSpec bound = spec;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            bound = bound.param(entry.getKey(), entry.getValue());
        }
        return bound;
    }

    private static BalanceMovement toMovement(ResultSet rs) throws SQLException {
        return new BalanceMovement(
                rs.getObject("id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                new YarnRef(rs.getString("yarn_type"), rs.getObject("lot_id", UUID.class)),
                rs.getObject("shipment_id", UUID.class),
                MovementKind.valueOf(rs.getString("kind")),
                rs.getBigDecimal("net_kg"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getObject("created_by", UUID.class),
                rs.getString("reason"),
                rs.getString("source_ref"));
    }
}
