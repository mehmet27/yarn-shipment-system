package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.ComparisonPort;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.ComparisonResult;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.model.Severity;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link ComparisonPort}'un JDBC uygulaması: girdi, uyuşmazlıklar, açılış kaydı ve {@code ComparisonCompleted}
 * outbox olayı aynı transaction'da yazılır (EVT-03, PERS-20).
 */
@Component
final class ComparisonAdapter implements ComparisonPort {

    private static final String COUNT_INPUTS = "SELECT count(*) FROM comparison_input WHERE shipment_id = :shipmentId";
    private static final String UNRESOLVED_SEVERITIES = """
            SELECT severity FROM discrepancy WHERE shipment_id = :shipmentId AND status IN ('OPEN', 'IN_REVIEW')""";
    private static final String INSERT_INPUT = """
            INSERT INTO comparison_input (id, shipment_id, dispatch_net_kg, delivery_net_kg, dispatch_bag_keys,
                                          delivery_bag_keys, seal_nos, received_at, site_id, subcontractor_id,
                                          shipment_no, created_at, created_by)
            VALUES (:id, :shipmentId, :dispatchNetKg, :deliveryNetKg, :dispatchKeys::jsonb, :deliveryKeys::jsonb,
                    :sealNos::jsonb, :receivedAt, :siteId, :subcontractorId, :shipmentNo, :at, :actor)""";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final UuidV7Generator ids;
    private final DiscrepancyWriter writer;

    ComparisonAdapter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids, DiscrepancyWriter writer) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
        this.writer = writer;
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
        DiscrepancyWriter.Stamp stamp = new DiscrepancyWriter.Stamp(comparison.actorId(), comparison.comparedAt());
        writer.setActor(comparison.actorId());
        List<Severity> openBefore = unresolvedSeverities(comparison.input().shipmentId());
        insertInput(comparison, Timestamp.from(comparison.comparedAt()));
        ComparisonInput input = comparison.input();
        DiscrepancyWriter.Target target = new DiscrepancyWriter.Target(
                input.siteId(), input.shipmentId(), input.shipmentNo(), input.subcontractorId());
        for (Finding finding : comparison.result().findings()) {
            writer.open(target, finding, comparison.dueAt(), stamp);
        }
        publish(comparison, openBefore, stamp);
    }

    private List<Severity> unresolvedSeverities(UUID shipmentId) {
        return jdbc.sql(UNRESOLVED_SEVERITIES)
                .param("shipmentId", shipmentId)
                .query((rs, rowNum) -> Severity.valueOf(rs.getString("severity")))
                .list();
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
                .param("siteId", input.siteId())
                .param("subcontractorId", input.subcontractorId())
                .param("shipmentNo", input.shipmentNo())
                .param("at", at)
                .param("actor", comparison.actorId())
                .update();
    }

    /** Teslimden önce açılmış çözülmemiş uyuşmazlık da sevkiyatı incelemeye alır; sayıya ve önceliğe dahildir. */
    private void publish(CompletedComparison comparison, List<Severity> openBefore, DiscrepancyWriter.Stamp stamp) {
        ComparisonResult result = comparison.result();
        boolean clean = result.clean() && openBefore.isEmpty();
        Optional<Severity> highest = Stream.concat(
                        result.findings().stream().map(Finding::severity), openBefore.stream())
                .max(Comparator.naturalOrder());
        ComparisonCompleted event = new ComparisonCompleted(
                result.shipmentId(),
                comparison.input().siteId(),
                comparison.comparedAt(),
                clean ? ComparisonCompleted.CLEAN : ComparisonCompleted.DISCREPANCY,
                result.quantityBand().name(),
                result.diffKg(),
                result.diffPct(),
                result.findings().size() + openBefore.size(),
                highest.map(Severity::name).orElse(null));
        writer.publish(result.shipmentId(), ComparisonCompleted.TYPE, event, stamp);
    }
}
