package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.UUID;
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

    private static final UUID SUBCONTRACTOR = UUID.randomUUID();

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
    @DisplayName("Uyuşmazlık açılıp çözülünce sevkiyatın açık sayısı ve çözüm zamanı yazılır; tekrar açılış saymaz")
    void shouldTrackDiscrepancyLifecycle() {
        UUID site = UUID.randomUUID();
        UUID shipment = UUID.randomUUID();
        UUID discrepancy = UUID.randomUUID();
        readModel.insertShipment(ShipmentFixtures.created(shipment, "SHP-D", site, SUBCONTRACTOR));
        readModel.insertDiscrepancy(ComparisonFixtures.opened(discrepancy, shipment, site));
        readModel.insertDiscrepancy(ComparisonFixtures.opened(discrepancy, shipment, site));

        assertThat(openCount(shipment)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT had_discrepancy FROM shipment_view WHERE shipment_id = ?", Boolean.class, shipment))
                .isTrue();
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
