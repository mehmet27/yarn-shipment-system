package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.TimeLimitRulePort;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@code time_limit_rule} tablosundaki yolda kalma sınırı; tolerans kuralı gibi sürümlenir. */
@Component
final class TimeLimitRuleAdapter implements TimeLimitRulePort {

    private static final String STAGE = "IN_TRANSIT";
    private static final String CURRENT = """
            SELECT max_hours
              FROM time_limit_rule
             WHERE stage = :stage AND valid_from <= :at AND (valid_to IS NULL OR valid_to > :at)""";
    private static final String HISTORY = """
            SELECT max_hours, valid_from, valid_to, created_by
              FROM time_limit_rule
             WHERE stage = :stage
             ORDER BY valid_from DESC
             LIMIT :limit""";
    private static final String CLOSE_CURRENT = """
            UPDATE time_limit_rule
               SET valid_to = :at, updated_at = :at, updated_by = :actor, version = version + 1
             WHERE stage = :stage AND valid_from <= :at AND (valid_to IS NULL OR valid_to > :at)""";
    private static final String INSERT = """
            INSERT INTO time_limit_rule (id, stage, max_hours, valid_from, created_at, created_by)
            VALUES (:id, :stage, :hours, :at, :at, :actor)""";

    private final JdbcClient jdbc;
    private final UuidV7Generator ids;
    private final DiscrepancyWriter writer;

    TimeLimitRuleAdapter(JdbcClient jdbc, UuidV7Generator ids, DiscrepancyWriter writer) {
        this.jdbc = jdbc;
        this.ids = ids;
        this.writer = writer;
    }

    @Override
    public Optional<TimeLimit> findTimeLimit(Instant at) {
        return jdbc.sql(CURRENT)
                .param("stage", STAGE)
                .param("at", Timestamp.from(at))
                .query((rs, rowNum) -> new TimeLimit(rs.getInt("max_hours")))
                .optional();
    }

    @Override
    public List<SettingVersion<TimeLimit>> findTimeLimitHistory(int limit) {
        return jdbc.sql(HISTORY)
                .param("stage", STAGE)
                .param("limit", limit)
                .query((rs, rowNum) -> RuleVersions.read(rs, new TimeLimit(rs.getInt("max_hours"))))
                .list();
    }

    @Override
    public void replaceTimeLimit(TimeLimit limit, UUID actorId, Instant at) {
        Timestamp timestamp = Timestamp.from(at);
        writer.setActor(actorId);
        jdbc.sql(CLOSE_CURRENT)
                .param("stage", STAGE)
                .param("at", timestamp)
                .param("actor", actorId)
                .update();
        jdbc.sql(INSERT)
                .param("id", ids.next())
                .param("stage", STAGE)
                .param("hours", limit.inTransitMaxHours())
                .param("at", timestamp)
                .param("actor", actorId)
                .update();
    }
}
