package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.WeightCheck;
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
            SELECT shipment_id, line_no, yarn_type_id, yarn_type_text, lot_id, net_kg
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
        jdbc.sql(LINES).param("ids", shipmentIds).query((rs, row) -> {
            RecipeLine line = new RecipeLine(
                    rs.getInt("line_no"),
                    rs.getObject("yarn_type_id", UUID.class),
                    rs.getString("yarn_type_text"),
                    rs.getObject("lot_id", UUID.class),
                    rs.getBigDecimal("net_kg"));
            lines.computeIfAbsent(rs.getObject("shipment_id", UUID.class), id -> new ArrayList<>())
                    .add(line);
            return line;
        }).list();
        Map<UUID, WeightCheck> checks = new HashMap<>();
        jdbc.sql(LATEST_CHECKS).param("ids", shipmentIds).query((rs, row) -> {
            WeightCheck check = new WeightCheck(
                    rs.getBigDecimal("empty_kg"),
                    rs.getBigDecimal("loaded_kg"),
                    rs.getBigDecimal("expected_kg"),
                    rs.getBigDecimal("tolerance_kg"),
                    rs.getString("shortfall_reason"));
            checks.put(rs.getObject("shipment_id", UUID.class), check);
            return check;
        }).list();
        return new ShipmentRecipes(lines, checks);
    }

    List<RecipeLine> lines(UUID shipmentId) {
        return lines.getOrDefault(shipmentId, List.of());
    }

    @Nullable
    WeightCheck weightCheck(UUID shipmentId) {
        return checks.get(shipmentId);
    }
}
