package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.reporting.application.port.out.ReadModelPort.PairComparison;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Eşleşme sayaçları ve uyuşmazlık okuma modeli; gerçek şemada, uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReadModelAdapter.class)
class ReadModelDiscrepancyTest {

    private static final UUID DRIVER = UUID.randomUUID();
    private static final UUID SUBCONTRACTOR = UUID.randomUUID();
    private static final int TWO = 2;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ReadModelAdapter readModel;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Aynı gün aynı eşleşmenin karşılaştırmaları tek satırda toplanır")
    void shouldAccumulatePairStats() {
        UUID site = UUID.randomUUID();
        LocalDate day = LocalDate.parse("2026-11-03");
        readModel.addPairComparison(
                new PairComparison(ShipmentFixtures.AT, day, site, DRIVER, SUBCONTRACTOR, 0, BigDecimal.ONE));
        readModel.addPairComparison(new PairComparison(
                ShipmentFixtures.AT, day, site, DRIVER, SUBCONTRACTOR, TWO, new BigDecimal("4.000")));

        Map<String, @Nullable Object> row = jdbc.queryForMap("SELECT * FROM pair_daily_stats WHERE site_id = ?", site);
        assertThat(row)
                .containsEntry("shipment_count", TWO)
                .containsEntry("discrepant_shipment_count", 1)
                .containsEntry("discrepancy_count", TWO);
        assertThat((BigDecimal) row.get("total_diff_kg")).isEqualByComparingTo("5.000");
    }

    @Test
    @DisplayName("Uyuşmazlık açılıp çözülünce sevkiyatın açık sayısı ve çözüm zamanı yazılır; tekrar açılış saymaz")
    void shouldTrackDiscrepancyLifecycle() {
        UUID site = UUID.randomUUID();
        UUID shipment = UUID.randomUUID();
        UUID discrepancy = UUID.randomUUID();
        readModel.insertShipment(ShipmentFixtures.created(shipment, "SHP-D", site, SUBCONTRACTOR));
        readModel.insertDiscrepancy(ComparisonFixtures.opened(discrepancy, shipment, site));
        readModel.insertDiscrepancy(ComparisonFixtures.opened(discrepancy, shipment, site));

        assertThat(openCount(shipment)).isEqualTo(1);
        readModel.saveDiscrepancyResolved(ComparisonFixtures.resolved(discrepancy, shipment));

        assertThat(openCount(shipment)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM discrepancy_view WHERE discrepancy_id = ? AND resolved_at IS NOT NULL",
                        Integer.class,
                        discrepancy))
                .isEqualTo(1);
    }

    private int openCount(UUID shipment) {
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT open_discrepancy_count FROM shipment_view WHERE shipment_id = ?", Integer.class, shipment));
    }
}
