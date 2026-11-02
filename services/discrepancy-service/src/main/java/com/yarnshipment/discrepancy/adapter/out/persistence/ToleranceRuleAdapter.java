package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.ToleranceRulePort;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@code tolerance_rule} tablosundaki genel kural (ürünsüz). Aralıklar çakışamaz (veritabanı kısıtı); yeni kural eski
 * kuralın {@code valid_to}'su kapatılıp yeni satırla yazılır.
 */
@Component
final class ToleranceRuleAdapter implements ToleranceRulePort {

    private static final String GENERAL_RULE = """
            SELECT green_max_pct, yellow_max_pct
              FROM tolerance_rule
             WHERE product_id IS NULL AND valid_from <= :at AND (valid_to IS NULL OR valid_to > :at)""";
    private static final String HISTORY = """
            SELECT green_max_pct, yellow_max_pct, valid_from, valid_to, created_by
              FROM tolerance_rule
             WHERE product_id IS NULL
             ORDER BY valid_from DESC
             LIMIT :limit""";
    private static final String CLOSE_CURRENT = """
            UPDATE tolerance_rule
               SET valid_to = :at, updated_at = :at, updated_by = :actor, version = version + 1
             WHERE product_id IS NULL AND valid_from <= :at AND (valid_to IS NULL OR valid_to > :at)""";
    private static final String INSERT = """
            INSERT INTO tolerance_rule (id, product_id, green_max_pct, yellow_max_pct, valid_from, created_at,
                                        created_by)
            VALUES (:id, NULL, :green, :yellow, :at, :at, :actor)""";

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;
    private final DiscrepancyWriter writer;

    ToleranceRuleAdapter(JdbcClient jdbc, UuidV7Generator ids, DiscrepancyWriter writer) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.writer = writer;
    }

    @Override
    public Optional<ToleranceBands> findGeneralRule(Instant at) {
        return jdbc.sql(GENERAL_RULE)
                .param("at", Timestamp.from(at))
                .query((rs, rowNum) -> bands(rs))
                .optional();
    }

    @Override
    public List<SettingVersion<ToleranceBands>> findGeneralRuleHistory(int limit) {
        return jdbc.sql(HISTORY)
                .param("limit", limit)
                .query((rs, rowNum) -> RuleVersions.read(rs, bands(rs)))
                .list();
    }

    @Override
    public void replaceGeneralRule(ToleranceBands bands, UUID actorId, Instant at) {
        Timestamp timestamp = Timestamp.from(at);
        writer.setActor(actorId);
        jdbc.sql(CLOSE_CURRENT).param("at", timestamp).param("actor", actorId).update();
        jdbc.sql(INSERT)
                .param("id", ids.next())
                .param("green", bands.greenMaxPct())
                .param("yellow", bands.yellowMaxPct())
                .param("at", timestamp)
                .param("actor", actorId)
                .update();
    }

    private static ToleranceBands bands(ResultSet rs) throws SQLException {
        return new ToleranceBands(
                rs.getObject("green_max_pct", BigDecimal.class), rs.getObject("yellow_max_pct", BigDecimal.class));
    }
}
