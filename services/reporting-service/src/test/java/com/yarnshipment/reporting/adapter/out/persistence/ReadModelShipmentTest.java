package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
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

/** Sevkiyat okuma modeli yazımları; gerçek şemada, uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReadModelAdapter.class)
class ReadModelShipmentTest {

    private static final UUID SITE = UUID.randomUUID();
    private static final UUID DRIVER = UUID.randomUUID();
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
    @DisplayName("Sevkiyat yaşam döngüsü satırı günceller; oluşturma olayı tekrarında ilk kayıt kalır")
    void shouldFollowShipmentLifecycle() {
        UUID id = UUID.randomUUID();
        readModel.insertShipment(ShipmentFixtures.created(id, "SHP-L", SITE, SUBCONTRACTOR));
        readModel.insertShipment(ShipmentFixtures.created(id, "SHP-X", SITE, SUBCONTRACTOR));
        assertThat(readModel.saveDispatched(ShipmentFixtures.dispatched(id, DRIVER)))
                .isTrue();
        assertThat(readModel.saveDelivery(ComparisonFixtures.delivery(id))).isTrue();
        assertThat(readModel.saveComparison(ComparisonFixtures.redComparison(id)))
                .isTrue();

        Map<String, @Nullable Object> row = shipment(id);
        assertThat(row).containsEntry("shipment_no", "SHP-L").containsEntry("status", "IN_REVIEW");
        assertThat(row).containsEntry("driver_id", DRIVER).containsEntry("had_discrepancy", true);
        assertThat((BigDecimal) row.get("diff_kg")).isEqualByComparingTo("40.000");
        assertThat(readModel.findParties(id)).hasValueSatisfying(parties -> {
            assertThat(parties.driverId()).isEqualTo(DRIVER);
            assertThat(parties.siteId()).isEqualTo(SITE);
        });
    }

    @Test
    @DisplayName("Tamamlanan sevkiyat sonradan iptal edilmez ve durumu geriye gitmez")
    void shouldNotMoveCompletedShipmentBack() {
        UUID id = UUID.randomUUID();
        readModel.insertShipment(ShipmentFixtures.created(id, "SHP-C", SITE, SUBCONTRACTOR));

        assertThat(readModel.saveCompleted(ShipmentFixtures.completed(id))).isTrue();
        assertThat(readModel.saveCancelled(ShipmentFixtures.cancelled(id))).isFalse();
        assertThat(readModel.saveArrived(ShipmentFixtures.arrived(id, DRIVER))).isTrue();
        assertThat(readModel.saveDelivery(ComparisonFixtures.delivery(id))).isTrue();

        assertThat(shipment(id)).containsEntry("status", "COMPLETED");
    }

    @Test
    @DisplayName("Olmayan sevkiyatın güncellemesi false döner")
    void shouldReportMissingShipment() {
        assertThat(readModel.saveCancelled(ShipmentFixtures.cancelled(UUID.randomUUID())))
                .isFalse();
        assertThat(readModel.findParties(UUID.randomUUID())).isEmpty();
    }

    private Map<String, @Nullable Object> shipment(UUID id) {
        return jdbc.queryForMap("SELECT * FROM shipment_view WHERE shipment_id = ?", id);
    }
}
