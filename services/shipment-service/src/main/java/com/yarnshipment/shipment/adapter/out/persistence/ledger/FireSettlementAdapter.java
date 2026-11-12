package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Component;

/** Fire dosyasının JDBC uygulaması. Toplamlar kayıt tablolarından hesaplanır, dosyada saklanmaz. */
@Component
final class FireSettlementAdapter implements FireSettlementPort {

    private static final String COLUMNS = """
            f.shipment_id, f.subcontractor_id, f.status, f.fire_rate, f.sent_kg, f.invoice_no, f.invoiced_on,
            f.invoiced_by, f.invoice_note""";
    private static final String OPEN = """
            INSERT INTO fire_settlement (shipment_id, subcontractor_id, status, fire_rate, sent_kg, created_at)
            VALUES (:shipmentId, :subcontractorId, :status, :fireRate, :sentKg, :createdAt)
            ON CONFLICT (shipment_id) DO NOTHING""";
    private static final String SAVE = """
            UPDATE fire_settlement SET status = :status, fire_rate = :fireRate, invoice_no = :invoiceNo,
                   invoiced_on = :invoicedOn, invoiced_by = :invoicedBy, invoice_note = :invoiceNote,
                   updated_at = :updatedAt
            WHERE shipment_id = :shipmentId""";
    private static final String TOTAL_COLUMNS = """
            coalesce((SELECT sum(r.net_kg) FROM yarn_return r
                      WHERE r.shipment_id = f.shipment_id AND r.direction = 'RETURN'), 0) AS returned_kg,
            greatest(0, coalesce((SELECT -sum(m.net_kg) FROM balance_movement m
                                  WHERE m.shipment_id = f.shipment_id AND m.kind = 'CLAIM_ADJUSTMENT'), 0)) AS claim_kg,
            coalesce((SELECT sum(fr.net_kg) FROM fabric_lot fr
                      WHERE fr.shipment_id = f.shipment_id AND fr.source = 'SUBCONTRACTOR'), 0) AS fabric_kg""";
    private static final String INSERT_RATE_CHANGE = """
            INSERT INTO fire_rate_change (id, shipment_id, old_rate, new_rate, reason, changed_by, changed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

    private final JdbcClient jdbc;

    FireSettlementAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void open(FireSettlement settlement, Instant at) {
        jdbc.sql(OPEN)
                .param("shipmentId", settlement.shipmentId())
                .param("subcontractorId", settlement.subcontractorId())
                .param("status", settlement.status().name())
                .param("fireRate", settlement.fireRate())
                .param("sentKg", settlement.sentKg())
                .param("createdAt", Timestamp.from(at))
                .update();
    }

    @Override
    public Optional<FireSettlement> find(UUID shipmentId) {
        return select(shipmentId, "");
    }

    @Override
    public Optional<FireSettlement> lock(UUID shipmentId) {
        return select(shipmentId, " FOR UPDATE");
    }

    @Override
    public void save(FireSettlement settlement, Instant at) {
        jdbc.sql(SAVE)
                .param("shipmentId", settlement.shipmentId())
                .param("status", settlement.status().name())
                .param("fireRate", settlement.fireRate())
                .param("invoiceNo", settlement.invoiceNo())
                .param("invoicedOn", settlement.invoicedOn())
                .param("invoicedBy", settlement.invoicedBy())
                .param("invoiceNote", settlement.invoiceNote())
                .param("updatedAt", Timestamp.from(at))
                .update();
    }

    @Override
    public FireTotals totals(UUID shipmentId) {
        return jdbc.sql("SELECT " + TOTAL_COLUMNS + " FROM fire_settlement f WHERE f.shipment_id = ?")
                .param(shipmentId)
                .query((rs, i) -> toTotals(rs))
                .single();
    }

    @Override
    public void recordRateChange(RateChange change) {
        jdbc.sql(INSERT_RATE_CHANGE)
                .params(
                        UUID.randomUUID(),
                        change.shipmentId(),
                        change.oldRate(),
                        change.newRate(),
                        change.reason(),
                        change.changedBy(),
                        Timestamp.from(change.changedAt()))
                .update();
    }

    @Override
    public List<ListedSettlement> findSettlements(ListFireSettlementsQuery query) {
        Filter filter = filterOf(query);
        return bind(
                        jdbc.sql("SELECT " + COLUMNS + ", s.shipment_no, s.subcontractor_title, " + TOTAL_COLUMNS
                                + " FROM fire_settlement f JOIN shipment s ON s.id = f.shipment_id" + filter.where
                                + " ORDER BY f.created_at DESC, f.shipment_id LIMIT :limit OFFSET :offset"),
                        filter.params)
                .param("limit", query.size())
                .param("offset", (long) query.page() * query.size())
                .query((rs, i) -> new ListedSettlement(
                        toSettlement(rs),
                        rs.getString("shipment_no"),
                        rs.getString("subcontractor_title"),
                        toTotals(rs)))
                .list();
    }

    @Override
    public long countSettlements(ListFireSettlementsQuery query) {
        Filter filter = filterOf(query);
        return bind(jdbc.sql("SELECT count(*) FROM fire_settlement f" + filter.where), filter.params)
                .query(Long.class)
                .single();
    }

    private Optional<FireSettlement> select(UUID shipmentId, String suffix) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM fire_settlement f WHERE f.shipment_id = ?" + suffix)
                .param(shipmentId)
                .query((rs, i) -> toSettlement(rs))
                .optional();
    }

    private static Filter filterOf(ListFireSettlementsQuery query) {
        Map<String, Object> params = new LinkedHashMap<>();
        List<String> conditions = new ArrayList<>();
        if (query.status() != null) {
            conditions.add("f.status = :status");
            params.put("status", query.status().name());
        }
        if (query.subcontractorId() != null) {
            conditions.add("f.subcontractor_id = :subcontractorId");
            params.put("subcontractorId", query.subcontractorId());
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new Filter(where, params);
    }

    private static StatementSpec bind(StatementSpec spec, Map<String, Object> params) {
        StatementSpec bound = spec;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            bound = bound.param(entry.getKey(), entry.getValue());
        }
        return bound;
    }

    private static FireSettlement toSettlement(ResultSet rs) throws SQLException {
        java.sql.Date invoicedOn = rs.getDate("invoiced_on");
        LocalDate date = invoicedOn == null ? null : invoicedOn.toLocalDate();
        return new FireSettlement(
                rs.getObject("shipment_id", UUID.class),
                rs.getObject("subcontractor_id", UUID.class),
                FireSettlementStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("fire_rate"),
                rs.getBigDecimal("sent_kg"),
                rs.getString("invoice_no"),
                date,
                rs.getObject("invoiced_by", UUID.class),
                rs.getString("invoice_note"));
    }

    private static FireTotals toTotals(ResultSet rs) throws SQLException {
        return new FireTotals(
                rs.getBigDecimal("returned_kg"), rs.getBigDecimal("claim_kg"), rs.getBigDecimal("fabric_kg"));
    }

    private record Filter(String where, Map<String, Object> params) {}
}
