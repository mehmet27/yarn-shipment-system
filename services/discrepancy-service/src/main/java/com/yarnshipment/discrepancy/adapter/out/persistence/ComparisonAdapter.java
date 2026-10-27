package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.ComparisonPort;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.ComparisonResult;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.model.Severity;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import com.yarnshipment.events.discrepancy.DiscrepancyOpened;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link ComparisonPort}'un JDBC uygulaması: girdi, uyuşmazlıklar, açılış kaydı ve {@code ComparisonCompleted}
 * outbox olayı aynı transaction'da yazılır (EVT-03, PERS-20).
 */
@Component
final class ComparisonAdapter implements ComparisonPort {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String OPENED = "OPENED";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String COUNT_INPUTS = "SELECT count(*) FROM comparison_input WHERE shipment_id = :shipmentId";
    private static final String INSERT_INPUT = """
            INSERT INTO comparison_input (id, shipment_id, dispatch_net_kg, delivery_net_kg, dispatch_bag_keys,
                                          delivery_bag_keys, seal_nos, received_at, created_at, created_by)
            VALUES (:id, :shipmentId, :dispatchNetKg, :deliveryNetKg, :dispatchKeys::jsonb, :deliveryKeys::jsonb,
                    :sealNos::jsonb, :receivedAt, :at, :actor)""";
    private static final String INSERT_DISCREPANCY = """
            INSERT INTO discrepancy (id, discrepancy_no, site_id, shipment_id, discrepancy_type, severity, status,
                                     rule_code, expected_value, actual_value, diff_kg, diff_pct, due_at, created_at,
                                     created_by)
            VALUES (:id, :no, :siteId, :shipmentId, :type, :severity, 'OPEN', :ruleCode, :expected, :actual, :diffKg,
                    :diffPct, :dueAt, :at, :actor)""";
    private static final String INSERT_ACTIVITY = """
            INSERT INTO discrepancy_activity (id, created_at, discrepancy_id, action_type, note, actor_id, occurred_at)
            VALUES (:id, :at, :discrepancyId, :action, :note, :actor, :at)""";
    private static final String INSERT_OUTBOX = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at)
            VALUES (:id, :at, :aggregateType, :shipmentId, :type, :payload::jsonb, :headers::jsonb, :at)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;

    ComparisonAdapter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    @Override
    public boolean hasComparison(UUID shipmentId) {
        long count = jdbc.sql(COUNT_INPUTS)
                .param("shipmentId", shipmentId)
                .query(Long.class)
                .single();
        return count > 0;
    }

    @Override
    public void saveComparison(CompletedComparison comparison) {
        Timestamp at = Timestamp.from(comparison.comparedAt());
        jdbc.sql(SET_ACTOR)
                .param("actor", comparison.actorId().toString())
                .query()
                .singleValue();
        insertInput(comparison, at);
        List<Finding> findings = comparison.result().findings();
        for (int i = 0; i < findings.size(); i++) {
            insertDiscrepancy(comparison, findings.get(i), discrepancyNo(comparison.input(), i), at);
        }
        publish(comparison);
    }

    private void insertInput(CompletedComparison comparison, Timestamp at) {
        ComparisonInput input = comparison.input();
        Map<String, Object> seals = new HashMap<>();
        seals.put("dispatch", input.dispatch().sealNo());
        seals.put("delivery", input.delivery().sealNo());
        seals.put("deliveryCondition", input.delivery().sealCondition().name());
        jdbc.sql(INSERT_INPUT)
                .param("id", ids.next())
                .param("shipmentId", input.shipmentId())
                .param("dispatchNetKg", input.dispatch().netKg())
                .param("deliveryNetKg", input.delivery().netKg())
                .param("dispatchKeys", jsonMapper.writeValueAsString(input.expectedKeys()))
                .param("deliveryKeys", jsonMapper.writeValueAsString(input.scannedKeys()))
                .param("sealNos", jsonMapper.writeValueAsString(seals))
                .param("receivedAt", Timestamp.from(input.receivedAt()))
                .param("at", at)
                .param("actor", comparison.actorId())
                .update();
    }

    private void insertDiscrepancy(CompletedComparison comparison, Finding finding, String no, Timestamp at) {
        ComparisonInput input = comparison.input();
        UUID id = ids.next();
        jdbc.sql(INSERT_DISCREPANCY)
                .param("id", id)
                .param("no", no)
                .param("siteId", input.siteId())
                .param("shipmentId", input.shipmentId())
                .param("type", finding.type().name())
                .param("severity", finding.severity().name())
                .param("ruleCode", finding.ruleCode())
                .param("expected", finding.expectedValue())
                .param("actual", finding.actualValue())
                .param("diffKg", finding.diffKg())
                .param("diffPct", finding.diffPct())
                .param("dueAt", Timestamp.from(comparison.dueAt()))
                .param("at", at)
                .param("actor", comparison.actorId())
                .update();
        insertOpenedActivity(comparison, finding, id, at);
        publishOpened(comparison, finding, id, no);
    }

    private void insertOpenedActivity(CompletedComparison comparison, Finding finding, UUID id, Timestamp at) {
        jdbc.sql(INSERT_ACTIVITY)
                .param("id", ids.next())
                .param("at", at)
                .param("discrepancyId", id)
                .param("action", OPENED)
                .param("note", finding.ruleCode())
                .param("actor", comparison.actorId())
                .update();
    }

    private void publishOpened(CompletedComparison comparison, Finding finding, UUID id, String no) {
        ComparisonInput input = comparison.input();
        DiscrepancyOpened opened = new DiscrepancyOpened(
                id,
                no,
                input.shipmentId(),
                input.siteId(),
                finding.type().name(),
                finding.severity().name(),
                finding.ruleCode(),
                comparison.dueAt());
        insertOutbox(comparison, DiscrepancyOpened.TYPE, opened);
    }

    /** Sevkiyat numarası ve sıra: aynı sevkiyatın uyuşmazlıkları birlikte okunur, numara tekrar üretilebilir. */
    private static String discrepancyNo(ComparisonInput input, int index) {
        String shipment = input.shipmentNo() == null ? input.shipmentId().toString() : input.shipmentNo();
        return "UY-" + shipment + "-" + (index + 1);
    }

    private void publish(CompletedComparison comparison) {
        ComparisonResult result = comparison.result();
        ComparisonCompleted event = new ComparisonCompleted(
                result.shipmentId(),
                comparison.input().siteId(),
                comparison.comparedAt(),
                result.clean() ? ComparisonCompleted.CLEAN : ComparisonCompleted.DISCREPANCY,
                result.quantityBand().name(),
                result.diffKg(),
                result.diffPct(),
                result.findings().size(),
                result.highestSeverity().map(Severity::name).orElse(null));
        insertOutbox(comparison, ComparisonCompleted.TYPE, event);
    }

    private void insertOutbox(CompletedComparison comparison, String type, Object event) {
        jdbc.sql(INSERT_OUTBOX)
                .param("id", ids.next())
                .param("at", Timestamp.from(comparison.comparedAt()))
                .param("aggregateType", AGGREGATE_TYPE)
                .param("shipmentId", comparison.input().shipmentId())
                .param("type", type)
                .param("payload", jsonMapper.writeValueAsString(event))
                .param(
                        "headers",
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", comparison.actorId().toString())))
                .update();
    }
}
