package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.events.discrepancy.DiscrepancyOpened;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Uyuşmazlık satırını, açılış kaydını ve {@code DiscrepancyOpened} outbox olayını yazar; karşılaştırma da karşılaştırma
 * dışı kurallar da (fotoğraf, zaman) aynı yolu kullanır. Numara sevkiyat numarası ve sevkiyattaki sıradır
 * ({@code UY-SV-3-1}); teslimden önce açılan uyuşmazlık sırayı alır, karşılaştırmanınkiler ardından gelir.
 */
@Component
final class DiscrepancyWriter {

    private static final String AGGREGATE_TYPE = "Shipment";
    private static final String OPENED = "OPENED";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', :actor, true)";
    private static final String COUNT_OF_SHIPMENT = "SELECT count(*) FROM discrepancy WHERE shipment_id = :shipmentId";
    private static final String INSERT_DISCREPANCY = """
            INSERT INTO discrepancy (id, discrepancy_no, site_id, shipment_id, subcontractor_id, discrepancy_type,
                                     severity, status, rule_code, expected_value, actual_value, diff_kg, diff_pct,
                                     due_at, created_at, created_by)
            VALUES (:id, :no, :siteId, :shipmentId, :subcontractorId, :type, :severity, 'OPEN', :ruleCode, :expected,
                    :actual, :diffKg, :diffPct, :dueAt, :at, :actor)""";
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

    DiscrepancyWriter(JdbcClient jdbc, JsonMapper jsonMapper, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.ids = ids;
    }

    /** Veritabanı audit tetikleyicisi yazan kişiyi bu ayardan okur; transaction sonuna kadar geçerlidir. */
    void setActor(UUID actorId) {
        jdbc.sql(SET_ACTOR).param("actor", actorId.toString()).query().singleValue();
    }

    void open(Target target, Finding finding, Instant dueAt, Stamp stamp) {
        UUID id = ids.next();
        String no = nextNo(target);
        insertDiscrepancy(new Row(id, no, target), finding, dueAt, stamp);
        jdbc.sql(INSERT_ACTIVITY)
                .param("id", ids.next())
                .param("at", Timestamp.from(stamp.at()))
                .param("discrepancyId", id)
                .param("action", OPENED)
                .param("note", finding.ruleCode())
                .param("actor", stamp.actorId())
                .update();
        DiscrepancyOpened opened = new DiscrepancyOpened(
                id,
                no,
                target.shipmentId(),
                target.siteId(),
                finding.type().name(),
                finding.severity().name(),
                finding.ruleCode(),
                dueAt);
        publish(target.shipmentId(), DiscrepancyOpened.TYPE, opened, stamp);
    }

    private void insertDiscrepancy(Row row, Finding finding, Instant dueAt, Stamp stamp) {
        jdbc.sql(INSERT_DISCREPANCY)
                .param("id", row.id())
                .param("no", row.no())
                .param("siteId", row.target().siteId())
                .param("shipmentId", row.target().shipmentId())
                .param("subcontractorId", row.target().subcontractorId())
                .param("type", finding.type().name())
                .param("severity", finding.severity().name())
                .param("ruleCode", finding.ruleCode())
                .param("expected", finding.expectedValue())
                .param("actual", finding.actualValue())
                .param("diffKg", finding.diffKg())
                .param("diffPct", finding.diffPct())
                .param("dueAt", Timestamp.from(dueAt))
                .param("at", Timestamp.from(stamp.at()))
                .param("actor", stamp.actorId())
                .update();
    }

    void publish(UUID shipmentId, String type, Object event, Stamp stamp) {
        jdbc.sql(INSERT_OUTBOX)
                .param("id", ids.next())
                .param("at", Timestamp.from(stamp.at()))
                .param("aggregateType", AGGREGATE_TYPE)
                .param("shipmentId", shipmentId)
                .param("type", type)
                .param("payload", jsonMapper.writeValueAsString(event))
                .param(
                        "headers",
                        jsonMapper.writeValueAsString(
                                Map.of("actorId", stamp.actorId().toString())))
                .update();
    }

    /** Sevkiyatta sıradaki numara; aynı anda iki yazım çakışırsa benzersizlik kısıtı ikincisini geri çevirir. */
    private String nextNo(Target target) {
        long existing = jdbc.sql(COUNT_OF_SHIPMENT)
                .param("shipmentId", target.shipmentId())
                .query(Long.class)
                .single();
        String shipment = target.shipmentNo() == null ? target.shipmentId().toString() : target.shipmentNo();
        return "UY-" + shipment + "-" + (existing + 1);
    }

    /**
     * Uyuşmazlığın açıldığı sevkiyat.
     *
     * @param siteId çıkış lokasyonu
     * @param shipmentId sevkiyat
     * @param shipmentNo sevkiyat numarası; eski teslim olaylarında boş, yerine kimlik yazılır
     * @param subcontractorId fasoncu
     */
    record Target(
            UUID siteId,
            UUID shipmentId,
            @Nullable String shipmentNo,
            @Nullable UUID subcontractorId) {}

    /**
     * Yazan kişi ve zaman.
     *
     * @param actorId kayıtların {@code created_by} değeri
     * @param at yazım zamanı
     */
    record Stamp(UUID actorId, Instant at) {}

    /** Yazılacak satırın kimliği, numarası ve sevkiyatı. */
    private record Row(UUID id, String no, Target target) {}
}
