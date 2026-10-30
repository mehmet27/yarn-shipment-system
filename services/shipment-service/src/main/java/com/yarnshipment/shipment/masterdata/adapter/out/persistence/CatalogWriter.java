package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.domain.catalog.DuplicateCodeException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Kodlu tanım tablolarının (iplik, kalite sınıfı) ortak yazımı: aktörü transaction'a işler, kod tekilliği ihlalini
 * {@link DuplicateCodeException}'a çevirir.
 */
final class CatalogWriter {

    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";

    private final JdbcClient jdbc;
    private final Clock clock;

    CatalogWriter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** {@code :id, :code, :name, :extra, :at, :actor} parametreli INSERT'i çalıştırır, yeni kimliği döner. */
    UUID insert(String sql, UUID actorId, Row row) {
        actAs(actorId);
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql(sql)
                    .param("id", id)
                    .param("code", row.code())
                    .param("name", row.name())
                    .param("extra", row.extra())
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", actorId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new DuplicateCodeException("code", row.code());
        }
        return id;
    }

    /** {@code :id, :name, :extra} parametreli güncellemeyi hazırlar. */
    JdbcClient.StatementSpec rewrite(String sql, UUID actorId, UUID id, Row row) {
        actAs(actorId);
        return jdbc.sql(sql).param("id", id).param("name", row.name()).param("extra", row.extra());
    }

    boolean setStatus(String sql, UUID actorId, UUID id, MasterDataStatus status) {
        actAs(actorId);
        return jdbc.sql(sql).param("active", status.isActive()).param("id", id).update() > 0;
    }

    private void actAs(UUID actorId) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
    }

    /**
     * Yazılan satır.
     *
     * @param code kod; güncellemede kullanılmaz
     * @param name ad
     * @param extra türe özgü isteğe bağlı alan (iplik numarası ya da etiket rengi)
     */
    record Row(String code, String name, @Nullable String extra) {}
}
