package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.fabric.FabricDrawEntry;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.FabricYarn;
import com.yarnshipment.shipment.domain.fabric.YarnDraw;
import com.yarnshipment.shipment.domain.fabric.YarnNames;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Kumaş girişlerinin okunması: kayıt, iplik oranları ve düşüldüğü sevkiyatlar. */
@Component
final class FabricReceiptReader {

    private static final String SELECT_RECEIPTS = """
            SELECT r.id, r.shipment_id, f.subcontractor_id, r.fabric_type, r.net_kg, r.roll_count, r.waybill_no,
                   r.occurred_at AS received_at, r.created_by, r.yarns
            FROM fabric_lot r JOIN fire_settlement f ON f.shipment_id = r.shipment_id
            WHERE r.shipment_id = ? AND r.source = 'SUBCONTRACTOR'
            ORDER BY r.occurred_at, r.created_at, r.id""";
    private static final String SELECT_DRAWS = """
            SELECT d.fabric_lot_id, d.source_shipment_id, s.shipment_no, d.yarn_type,
                   coalesce(p.name, d.yarn_type) AS yarn_name, d.lot_id, pl.lot_no, d.net_kg
            FROM fabric_yarn_draw d
            JOIN shipment s ON s.id = d.source_shipment_id
            LEFT JOIN md_product p ON p.id::text = d.yarn_type
            LEFT JOIN md_production_lot pl ON pl.id = d.lot_id
            WHERE d.fabric_lot_id IN (:ids)
            ORDER BY d.fabric_lot_id, d.position""";
    private static final String SELECT_DRAWS_FROM = """
            SELECT l.id, l.fabric_type, l.shipment_id AS entry_id, es.shipment_no AS entry_no,
                   coalesce(p.name, d.yarn_type) AS yarn_name, pl.lot_no, d.net_kg, l.occurred_at
            FROM fabric_yarn_draw d
            JOIN fabric_lot l ON l.id = d.fabric_lot_id
            JOIN shipment es ON es.id = l.shipment_id
            LEFT JOIN md_product p ON p.id::text = d.yarn_type
            LEFT JOIN md_production_lot pl ON pl.id = d.lot_id
            WHERE d.source_shipment_id = ?
            ORDER BY l.occurred_at, l.created_at, l.id, d.position""";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final JdbcClient jdbc;

    FabricReceiptReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<FabricReceipt> findByShipment(UUID shipmentId) {
        List<ReceiptRow> rows = jdbc.sql(SELECT_RECEIPTS)
                .param(shipmentId)
                .query((rs, i) -> toRow(rs))
                .list();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<DrawRow>> draws =
                drawsOf(rows.stream().map(ReceiptRow::id).toList());
        return rows.stream()
                .map(row -> toReceipt(row, draws.getOrDefault(row.id(), List.of())))
                .toList();
    }

    List<FabricDrawEntry> findDrawsFrom(UUID sourceShipmentId) {
        return jdbc.sql(SELECT_DRAWS_FROM)
                .param(sourceShipmentId)
                .query((rs, i) -> toDrawEntry(rs))
                .list();
    }

    private static FabricDrawEntry toDrawEntry(ResultSet rs) throws SQLException {
        return new FabricDrawEntry(
                rs.getObject("id", UUID.class),
                rs.getString("fabric_type"),
                rs.getObject("entry_id", UUID.class),
                rs.getString("entry_no"),
                rs.getString("yarn_name"),
                rs.getString("lot_no"),
                Kilograms.round(rs.getBigDecimal("net_kg")),
                rs.getTimestamp("occurred_at").toInstant());
    }

    private Map<UUID, List<DrawRow>> drawsOf(List<UUID> fabricIds) {
        Map<UUID, List<DrawRow>> byFabric = new LinkedHashMap<>();
        jdbc.sql(SELECT_DRAWS)
                .param("ids", fabricIds)
                .query((rs, i) -> toDrawRow(rs))
                .list()
                .forEach(draw -> byFabric.computeIfAbsent(draw.fabricId(), id -> new ArrayList<>())
                        .add(draw));
        return byFabric;
    }

    private static DrawRow toDrawRow(ResultSet rs) throws SQLException {
        YarnDraw draw = new YarnDraw(
                rs.getObject("source_shipment_id", UUID.class),
                rs.getString("shipment_no"),
                rs.getString("yarn_type"),
                rs.getObject("lot_id", UUID.class),
                Kilograms.round(rs.getBigDecimal("net_kg")));
        UUID fabricId = rs.getObject("fabric_lot_id", UUID.class);
        return new DrawRow(fabricId, rs.getString("yarn_name"), rs.getString("lot_no"), draw);
    }

    private static ReceiptRow toRow(ResultSet rs) throws SQLException {
        return new ReceiptRow(
                rs.getObject("id", UUID.class),
                rs.getObject("shipment_id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                rs.getString("fabric_type"),
                rs.getBigDecimal("net_kg"),
                (Integer) rs.getObject("roll_count"),
                rs.getString("waybill_no"),
                rs.getTimestamp("received_at").toInstant(),
                rs.getObject("created_by", UUID.class),
                rs.getString("yarns"));
    }

    private static FabricReceipt toReceipt(ReceiptRow row, List<DrawRow> draws) {
        return new FabricReceipt(
                row.id(),
                row.shipmentId(),
                row.subcontractorId(),
                row.fabricType(),
                row.netKg(),
                row.rollCount(),
                row.waybillNo(),
                row.receivedAt(),
                readYarns(row.yarns(), row.netKg(), draws),
                row.createdBy());
    }

    /**
     * Kayıttaki iplikler; oranı olmayan eski kayıtta oran kilodan çıkarılır. Düşümler iplik adı ve partisine göre
     * iplikle eşlenir.
     */
    private static List<FabricYarn> readYarns(String json, BigDecimal fabricKg, List<DrawRow> draws) {
        try {
            List<FabricYarn> yarns = new ArrayList<>();
            for (JsonNode node : JSON.readTree(json)) {
                yarns.add(readYarn(node, fabricKg, draws));
            }
            return yarns;
        } catch (JacksonException e) {
            throw new IllegalStateException("fabric_lot.yarns is not valid JSON", e);
        }
    }

    private static FabricYarn readYarn(JsonNode node, BigDecimal fabricKg, List<DrawRow> draws) {
        String name = node.get("yarnType").asString();
        UUID lotId =
                node.hasNonNull("lotId") ? UUID.fromString(node.get("lotId").asString()) : null;
        BigDecimal kg = Kilograms.round(node.get("netKg").decimalValue());
        BigDecimal ratio = node.hasNonNull("ratioPct")
                ? node.get("ratioPct").decimalValue().setScale(2, RoundingMode.HALF_UP)
                : kg.multiply(HUNDRED).divide(fabricKg, 2, RoundingMode.HALF_UP);
        List<DrawRow> own =
                draws.stream().filter(draw -> draw.matches(name, lotId)).toList();
        String lotNo = own.stream()
                .map(DrawRow::lotNo)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return new FabricYarn(
                name, lotId, lotNo, ratio, kg, own.stream().map(DrawRow::draw).toList());
    }

    private record ReceiptRow(
            UUID id,
            UUID shipmentId,
            UUID subcontractorId,
            String fabricType,
            BigDecimal netKg,
            @Nullable Integer rollCount,
            String waybillNo,
            Instant receivedAt,
            UUID createdBy,
            String yarns) {}

    private record DrawRow(
            UUID fabricId, String yarnName, @Nullable String lotNo, YarnDraw draw) {

        boolean matches(String name, @Nullable UUID lotId) {
            return YarnNames.same(yarnName, name) && Objects.equals(draw.lotId(), lotId);
        }
    }
}
