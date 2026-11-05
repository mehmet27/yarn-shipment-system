package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.BagVoidPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagNotVoidableException;
import com.yarnshipment.bag.domain.model.BagStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link BagVoidPort}'un JDBC uygulaması. Durum güncellemesi {@code status = 'CREATED'} koşuluyla yapılır; çuval bu
 * arada sevkiyata ayrıldıysa satır güncellenmez ve iptal reddedilir.
 */
@Component
final class BagVoidAdapter implements BagVoidPort {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String MARK_VOID = """
            UPDATE bag SET status = 'VOID', updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :id AND status = 'CREATED'
            """;
    private static final String INSERT = """
            INSERT INTO bag_void (id, bag_id, reason, voided_at, voided_by, created_at, created_by)
            VALUES (:id, :bagId, :reason, :at, :actor, :at, :actor)
            """;

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;

    BagVoidAdapter(JdbcClient jdbc, UuidV7Generator ids) {
        this.jdbc = jdbc;
        this.ids = ids;
    }

    @Override
    public void voidBags(List<Bag> bags, String reason, UUID actorUserId, Instant at) {
        if (bags.isEmpty()) {
            return;
        }
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
        Timestamp now = Timestamp.from(at);
        for (Bag bag : bags) {
            int updated = jdbc.sql(MARK_VOID)
                    .param("at", now)
                    .param("actor", actorUserId)
                    .param("id", bag.id())
                    .update();
            if (updated == 0) {
                throw new BagNotVoidableException(bag.uniqueKey(), BagStatus.RESERVED);
            }
            jdbc.sql(INSERT)
                    .param("id", ids.next())
                    .param("bagId", bag.id())
                    .param("reason", reason)
                    .param("at", now)
                    .param("actor", actorUserId)
                    .update();
        }
    }
}
