package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Admin'in değiştirdiği kural sürümleri (tolerans, süre sınırı); gerçek şemada, uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ToleranceRuleAdapter.class, TimeLimitRuleAdapter.class, DiscrepancyWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class RuleVersionAdaptersTest {

    private static final UUID ADMIN = UUID.fromString("0192f0a0-0000-7000-8000-00000000d0a1");
    private static final UUID SYSTEM = new UUID(0L, 0L);
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final int HISTORY = 10;
    private static final int NEW_HOURS = 36;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ToleranceRuleAdapter toleranceRules;

    @Autowired
    private TimeLimitRuleAdapter timeLimits;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Yeni tolerans eski kuralı kapatır; geçmiş en yeniden eskiye, giren kişiyle döner")
    void shouldReplaceToleranceRule() {
        ToleranceBands bands = new ToleranceBands(new BigDecimal("0.80"), new BigDecimal("2.00"));

        toleranceRules.replaceGeneralRule(bands, ADMIN, NOW);

        assertThat(toleranceRules.findGeneralRule(NOW)).contains(bands);
        assertThat(toleranceRules.findGeneralRule(NOW.minusSeconds(1))).contains(ToleranceBands.REPORT_DEFAULT);
        List<SettingVersion<ToleranceBands>> history = toleranceRules.findGeneralRuleHistory(HISTORY);
        assertThat(history).extracting(SettingVersion::setBy).containsExactly(ADMIN, SYSTEM);
        assertThat(history.get(0).validTo()).isNull();
        assertThat(history.get(1).validTo()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Migration'daki 24 saatlik sınır okunur; yeni sınır eskisini kapatır")
    void shouldReplaceTimeLimit() {
        assertThat(timeLimits.findTimeLimit(NOW)).contains(TimeLimit.REPORT_DEFAULT);

        timeLimits.replaceTimeLimit(new TimeLimit(NEW_HOURS), ADMIN, NOW);

        assertThat(timeLimits.findTimeLimit(NOW)).contains(new TimeLimit(NEW_HOURS));
        assertThat(timeLimits.findTimeLimitHistory(HISTORY))
                .extracting(version -> version.value())
                .containsExactly(new TimeLimit(NEW_HOURS), TimeLimit.REPORT_DEFAULT);
    }
}
