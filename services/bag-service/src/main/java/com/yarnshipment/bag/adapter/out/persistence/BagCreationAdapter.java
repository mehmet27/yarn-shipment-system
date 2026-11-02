package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.BagCreationPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LockedLot;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link BagCreationPort}'un JDBC uygulaması. Anahtar çakışırsa (40 bitte çok nadir) satır eklenmez ve yeni anahtarla
 * tekrar denenir; transaction bozulmaz. Parti ve kalite {@code lot_quality_lock}'tan kopyalanır, yabancı anahtar
 * ({@code bag_quality_from_locked_lot}) kilitsiz partiyi veritabanında da reddeder.
 */
@Component
final class BagCreationAdapter implements BagCreationPort {

    private static final int MAX_ATTEMPTS = 5;
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String INSERT = """
            INSERT INTO bag (id, site_id, unique_key, package_type, lot_id, lot_no, product_id, quality_grade_code,
                             status, created_at, created_by)
            VALUES (:id, :siteId, :key, :packageType, :lotId, :lotNo, :productId, :grade, :status, :at, :actor)
            ON CONFLICT (unique_key) DO NOTHING
            """;

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;
    private final UniqueKeyGenerator keys;

    BagCreationAdapter(JdbcClient jdbc, UuidV7Generator ids, UniqueKeyGenerator keys) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.keys = keys;
    }

    @Override
    public List<Bag> create(NewBags newBags, UUID actorUserId, Instant at) {
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
        Timestamp now = Timestamp.from(at);
        List<Bag> created = new ArrayList<>(newBags.count());
        for (int index = 0; index < newBags.count(); index++) {
            created.add(insertOne(newBags, actorUserId, now));
        }
        return List.copyOf(created);
    }

    private Bag insertOne(NewBags newBags, UUID actorUserId, Timestamp at) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            UUID id = ids.next();
            String key = keys.next();
            int inserted = jdbc.sql(INSERT)
                    .param("id", id)
                    .param("siteId", newBags.lot().siteId())
                    .param("key", key)
                    .param("packageType", newBags.packageType().name())
                    .param("lotId", newBags.lot().lotId())
                    .param("lotNo", newBags.lot().lotNo())
                    .param("productId", newBags.lot().productId())
                    .param("grade", newBags.lot().qualityGradeCode())
                    .param("status", BagStatus.CREATED.name())
                    .param("at", at)
                    .param("actor", actorUserId)
                    .update();
            if (inserted == 1) {
                return created(id, key, newBags);
            }
        }
        throw new IllegalStateException("Could not generate a free unique key after " + MAX_ATTEMPTS + " attempts");
    }

    private static Bag created(UUID id, String key, NewBags newBags) {
        LockedLot lot = newBags.lot();
        return new Bag(
                id,
                lot.siteId(),
                key,
                newBags.packageType(),
                lot.lotNo(),
                lot.qualityGradeCode(),
                BagStatus.CREATED,
                null);
    }
}
