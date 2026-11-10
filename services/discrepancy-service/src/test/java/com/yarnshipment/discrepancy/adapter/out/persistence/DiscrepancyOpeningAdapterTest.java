package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.out.OpenedDiscrepancy;
import com.yarnshipment.discrepancy.domain.model.ComparisonRule;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.rule.EvidenceChecks;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Duration;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Kurallarla açılan uyuşmazlıkların numaralanması ve olayları; gerçek şemada, uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DiscrepancyOpeningAdapter.class, DiscrepancyWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class DiscrepancyOpeningAdapterTest {

    private static final UUID SYSTEM = new UUID(0L, 0L);
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final Duration DUE = Duration.ofDays(7);
    private static final int EXPECTED_OPENED = 2;
    private static final WatchedShipment SHIPMENT = TestComparisons.watched();

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DiscrepancyOpeningAdapter opening;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Aynı sevkiyatta açılan uyuşmazlıklar sırayla numaralanır ve her biri için olay yazılır")
    void shouldNumberDiscrepanciesOfShipment() {
        opening.openDiscrepancy(new OpenedDiscrepancy(
                SHIPMENT, EvidenceChecks.reused(UUID.randomUUID(), UUID.randomUUID()), NOW, NOW.plus(DUE), SYSTEM));
        opening.openDiscrepancy(new OpenedDiscrepancy(
                SHIPMENT, Finding.of(ComparisonRule.TIME_IN_TRANSIT_EXCEEDED, null, null), NOW, NOW.plus(DUE), SYSTEM));

        assertThat(jdbc.queryForList(
                        "SELECT discrepancy_no || ':' || rule_code || ':' || coalesce(subcontractor_id::text, '-')"
                                + " FROM discrepancy ORDER BY discrepancy_no",
                        String.class))
                .containsExactly(
                        "UY-SV-3-1:PHOTO_REUSED:" + TestComparisons.SUBCONTRACTOR,
                        "UY-SV-3-2:TIME_IN_TRANSIT_EXCEEDED:" + TestComparisons.SUBCONTRACTOR);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE event_type LIKE '%.discrepancy-opened.v1'",
                        Integer.class))
                .isEqualTo(EXPECTED_OPENED);
    }
}
