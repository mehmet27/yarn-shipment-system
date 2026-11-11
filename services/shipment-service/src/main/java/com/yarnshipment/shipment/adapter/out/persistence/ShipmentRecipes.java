package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.WeightCheck;
import com.yarnshipment.shipment.domain.model.WidthType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Bir sayfadaki sevkiyatların reçete satırları ve son kantar karşılaştırmaları; sayfa başına iki sorguyla okunur. */
final class ShipmentRecipes {

    private static final String LINES = """
            SELECT shipment_id, line_no, yarn_type_id, yarn_type_text, lot_id, net_kg,
                   fabric_type, lycra, width_type, yarn_length, puss_fine, unit_price
            FROM shipment_line WHERE shipment_id IN (:ids) ORDER BY shipment_id, line_no""";
    private static final String LATEST_CHECKS = """
            SELECT DISTINCT ON (shipment_id) shipment_id, empty_kg, loaded_kg, expected_kg, tolerance_kg,
                   shortfall_reason
            FROM weight_check WHERE shipment_id IN (:ids) ORDER BY shipment_id, created_at DESC, id DESC""";

    private final Map<UUID, List<RecipeLine>> lines;
    private final Map<UUID, WeightCheck> checks;

    private ShipmentRecipes(Map<UUID, List<RecipeLine>> lines, Map<UUID, WeightCheck> checks) {
        this.lines = lines;
        this.checks = checks;
    }

    static ShipmentRecipes load(JdbcClient jdbc, Collection<UUID> shipmentIds) {
        Map<UUID, List<RecipeLine>> lines = new HashMap<>();
        jdbc.sql(LINES)
                .param("ids", shipmentIds)
                .query((rs, row) -> collectLine(lines, rs))
                .list();
        Map<UUID, WeightCheck> checks = new HashMap<>();
        jdbc.sql(LATEST_CHECKS)
                .param("ids", shipmentIds)
                .query((rs, row) -> collectCheck(checks, rs))
                .list();
        return new ShipmentRecipes(lines, checks);
    }

    private static RecipeLine collectLine(Map<UUID, List<RecipeLine>> lines, ResultSet rs) throws SQLException {
        RecipeLine line = new RecipeLine(
                rs.getInt("line_no"),
                rs.getObject("yarn_type_id", UUID.class),
                rs.getString("yarn_type_text"),
                rs.getObject("lot_id", UUID.class),
                rs.getBigDecimal("net_kg"),
                rs.getString("fabric_type"),
                rs.getString("lycra"),
                widthType(rs.getString("width_type")),
                rs.getString("yarn_length"),
                rs.getString("puss_fine"),
                rs.getBigDecimal("unit_price"));
        lines.computeIfAbsent(rs.getObject("shipment_id", UUID.class), id -> new ArrayList<>())
                .add(line);
        return line;
    }

    private static @Nullable WidthType widthType(@Nullable String value) {
        return value == null ? null : WidthType.valueOf(value);
    }

    private static WeightCheck collectCheck(Map<UUID, WeightCheck> checks, ResultSet rs) throws SQLException {
        WeightCheck check = new WeightCheck(
                rs.getBigDecimal("empty_kg"),
                rs.getBigDecimal("loaded_kg"),
                rs.getBigDecimal("expected_kg"),
                rs.getBigDecimal("tolerance_kg"),
                rs.getString("shortfall_reason"));
        checks.put(rs.getObject("shipment_id", UUID.class), check);
        return check;
    }

    List<RecipeLine> lines(UUID shipmentId) {
        return lines.getOrDefault(shipmentId, List.of());
    }

    @Nullable
    WeightCheck weightCheck(UUID shipmentId) {
        return checks.get(shipmentId);
    }
}
