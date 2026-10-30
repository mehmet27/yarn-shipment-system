package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.LabelPrintPort;
import com.yarnshipment.bag.domain.model.BagLabel;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link LabelPrintPort}'un JDBC uygulaması. Çuval satırı kimliğe göre sıralı kilitlenir (rezervasyonla aynı sıra);
 * {@code (bag_id, print_no)} unique kısıtı son güvencedir.
 */
@Component
final class LabelPrintAdapter implements LabelPrintPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String LOCK = "SELECT id FROM bag WHERE id IN (:ids) ORDER BY id FOR UPDATE";
    private static final String COUNT = """
            SELECT bag_id, max(print_no) AS printed FROM label_print WHERE bag_id IN (:ids) GROUP BY bag_id
            """;
    private static final String INSERT = """
            INSERT INTO label_print (id, bag_id, print_no, printed_at, printed_by, reprint_reason, created_at,
                                     created_by)
            VALUES (:id, :bagId, :printNo, :at, :actor, :reason, :at, :actor)
            """;

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;

    LabelPrintAdapter(JdbcClient jdbc, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public Map<UUID, Integer> lockAndCountPrints(Collection<UUID> bagIds) {
        if (bagIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> idList = List.copyOf(bagIds);
        jdbc.sql(LOCK).param("ids", idList).query().listOfRows();
        Map<UUID, Integer> printed = new HashMap<>();
        jdbc.sql(COUNT)
                .param("ids", idList)
                .query(rs -> {
                    printed.put(rs.getObject("bag_id", UUID.class), rs.getInt("printed"));
                });
        return Map.copyOf(printed);
    }

    @Override
    public void record(List<BagLabel> labels, @Nullable String reprintReason, UUID actorUserId, Instant at) {
        if (labels.isEmpty()) {
            return;
        }
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
        Timestamp now = Timestamp.from(at);
        for (BagLabel label : labels) {
            jdbc.sql(INSERT)
                    .param("id", ids.next())
                    .param("bagId", label.bag().id())
                    .param("printNo", label.printNo())
                    .param("at", now)
                    .param("actor", actorUserId)
                    .param("reason", label.isReprint() ? reprintReason : null)
                    .update();
        }
    }
}
