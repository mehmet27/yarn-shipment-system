package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.application.port.out.ToleranceRulePort;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@code tolerance_rule} tablosundan, verilen anda geçerli genel kural. Aralıklar çakışamaz (veritabanı kısıtı). */
@Component
final class ToleranceRuleAdapter implements ToleranceRulePort {

    private static final String GENERAL_RULE = """
            SELECT green_max_pct, yellow_max_pct
              FROM tolerance_rule
             WHERE product_id IS NULL AND valid_from <= :at AND (valid_to IS NULL OR valid_to > :at)""";

    private final JdbcClient jdbc;

    ToleranceRuleAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ToleranceBands> findGeneralRule(Instant at) {
        return jdbc.sql(GENERAL_RULE)
                .param("at", Timestamp.from(at))
                .query((rs, rowNum) -> new ToleranceBands(
                        rs.getObject("green_max_pct", BigDecimal.class),
                        rs.getObject("yellow_max_pct", BigDecimal.class)))
                .optional();
    }
}
