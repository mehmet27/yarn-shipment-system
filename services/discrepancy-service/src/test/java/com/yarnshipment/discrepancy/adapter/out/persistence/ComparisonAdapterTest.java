package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.domain.model.Comparison;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.DispatchFigures;
import com.yarnshipment.discrepancy.domain.model.DocumentFigures;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Karşılaştırma kaydı ve tolerans kuralı, migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ComparisonAdapter.class, ToleranceRuleAdapter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class ComparisonAdapterTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final Instant DUE = Instant.parse("2026-10-30T09:00:00Z");

    @Autowired
    private ComparisonAdapter comparisons;

    @Autowired
    private ToleranceRuleAdapter toleranceRules;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Migration'daki genel kural okunur; geçerlilik başlangıcından önce kural yoktur")
    void shouldReadGeneralRule() {
        assertThat(toleranceRules.findGeneralRule(NOW)).contains(ToleranceBands.REPORT_DEFAULT);
        assertThat(toleranceRules.findGeneralRule(Instant.parse("2025-12-31T00:00:00Z")))
                .isEmpty();
    }

    @Test
    @DisplayName("Girdi, uyuşmazlıklar, açılış kaydı ve sonuç olayı birlikte yazılır; numaralar sevkiyattan türer")
    void shouldSaveComparisonWithDiscrepancies() {
        // given: çıkış tartısı yok ve imzalı belge yok
        ComparisonInput input = TestComparisons.input(
                new DispatchFigures(null, TestComparisons.BAGS, TestComparisons.SEAL),
                TestComparisons.delivery(TestComparisons.DISPATCH_KG),
                TestComparisons.KEYS,
                DocumentFigures.NONE);

        // when
        comparisons.saveComparison(completed(input));

        // then
        assertThat(comparisons.hasComparison(TestComparisons.SHIPMENT)).isTrue();
        assertThat(jdbc.queryForList(
                        "SELECT discrepancy_no || ':' || rule_code || ':' || severity FROM discrepancy"
                                + " ORDER BY discrepancy_no",
                        String.class))
                .containsExactly("UY-SV-3-1:QUANTITY_DISPATCH_MISSING:RED", "UY-SV-3-2:DOCUMENT_MISSING:YELLOW");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM discrepancy_activity WHERE action_type = 'OPENED'", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT payload ->> 'outcome' || ':' || (payload ->> 'highestSeverity') FROM outbox_event"
                                + " WHERE event_type = ?",
                        String.class,
                        ComparisonCompleted.TYPE))
                .isEqualTo("DISCREPANCY:RED");
    }

    @Test
    @DisplayName("Temiz karşılaştırmada uyuşmazlık açılmaz, olay temiz sonuç taşır")
    void shouldSaveCleanComparison() {
        comparisons.saveComparison(completed(TestComparisons.clean()));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM discrepancy", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT payload ->> 'outcome' FROM outbox_event WHERE aggregate_id = ?",
                        String.class,
                        TestComparisons.SHIPMENT))
                .isEqualTo(ComparisonCompleted.CLEAN);
    }

    @Test
    @DisplayName("Karşılaştırması olmayan sevkiyatta tekrar kontrolü yanlış döner")
    void shouldReportMissingComparison() {
        assertThat(comparisons.hasComparison(TestComparisons.SHIPMENT)).isFalse();
    }

    private static CompletedComparison completed(ComparisonInput input) {
        return new CompletedComparison(
                input, Comparison.compare(input, ToleranceBands.REPORT_DEFAULT), NOW, DUE, ACTOR);
    }
}
