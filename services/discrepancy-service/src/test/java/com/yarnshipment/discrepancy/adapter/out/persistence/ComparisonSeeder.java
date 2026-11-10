package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.domain.model.Finding;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Eski teslim karşılaştırmasından kalan {@code comparison_input} satırını ve verilen bulguların uyuşmazlıklarını
 * gerçek şemaya yazar. Teslim akışı kaldırıldığı için tablo yalnızca geçmiş kayıtlar için okunur; testler bu satırları
 * elle hazırlar.
 */
final class ComparisonSeeder {

    private static final String INSERT_INPUT = """
            INSERT INTO comparison_input (id, shipment_id, dispatch_net_kg, delivery_net_kg, dispatch_bag_keys,
                                          delivery_bag_keys, seal_nos, received_at, site_id, subcontractor_id,
                                          shipment_no, created_at, created_by)
            VALUES (:id, :shipmentId, :dispatchNetKg, :deliveryNetKg, :keys::jsonb, :keys::jsonb, '{}'::jsonb,
                    :receivedAt, :siteId, :subcontractorId, :shipmentNo, :at, :actor)""";
    private static final String KEYS_JSON = "[\"" + String.join("\",\"", TestComparisons.KEYS) + "\"]";

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;
    private final DiscrepancyWriter writer;

    ComparisonSeeder(JdbcClient jdbc, UuidV7Generator ids, DiscrepancyWriter writer) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.writer = writer;
    }

    /** Teslim edilmiş {@link TestComparisons#SHIPMENT}; bulgular uyuşmazlık olarak açılır. */
    void seed(@Nullable BigDecimal dispatchNetKg, List<Finding> findings, Instant at, Instant due) {
        UUID actor = SeededDiscrepancies.ACTOR;
        writer.setActor(actor);
        jdbc.sql(INSERT_INPUT)
                .param("id", ids.next())
                .param("shipmentId", TestComparisons.SHIPMENT)
                .param("dispatchNetKg", dispatchNetKg)
                .param("deliveryNetKg", TestComparisons.DISPATCH_KG)
                .param("keys", KEYS_JSON)
                .param("receivedAt", Timestamp.from(TestComparisons.RECEIVED_AT))
                .param("siteId", TestComparisons.SITE)
                .param("subcontractorId", TestComparisons.SUBCONTRACTOR)
                .param("shipmentNo", TestComparisons.SHIPMENT_NO)
                .param("at", Timestamp.from(at))
                .param("actor", actor)
                .update();
        DiscrepancyWriter.Target target = new DiscrepancyWriter.Target(
                TestComparisons.SITE,
                TestComparisons.SHIPMENT,
                TestComparisons.SHIPMENT_NO,
                TestComparisons.SUBCONTRACTOR);
        DiscrepancyWriter.Stamp stamp = new DiscrepancyWriter.Stamp(actor, at);
        for (Finding finding : findings) {
            writer.open(target, finding, due, stamp);
        }
    }
}
