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
    private static final BigDecimal DISPATCH_NET = new BigDecimal("500.000");
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
    @DisplayName(
            "Çıkış ve tamamlanma satırı günceller; tamamlanınca ulaşan kilo çıkış netidir; ilk oluşturma kaydı kalır")
    void shouldFollowShipmentLifecycle() {
        UUID id = UUID.randomUUID();
        readModel.insertShipment(ShipmentFixtures.created(id, "SHP-L", SITE, SUBCONTRACTOR));
        readModel.insertShipment(ShipmentFixtures.created(id, "SHP-X", SITE, SUBCONTRACTOR));
        assertThat(readModel.saveDispatched(ShipmentFixtures.dispatched(id, DISPATCH_NET)))
                .isTrue();

        Map<String, @Nullable Object> row = shipment(id);
        assertThat(row).containsEntry("shipment_no", "SHP-L").containsEntry("status", "AWAITING_DELIVERY");
        assertThat((BigDecimal) row.get("dispatch_net_kg")).isEqualByComparingTo(DISPATCH_NET);
        assertThat(row.get("delivery_net_kg")).isNull();

        assertThat(readModel.saveCompleted(ShipmentFixtures.completed(id))).isTrue();
        row = shipment(id);
        assertThat(row).containsEntry("status", "COMPLETED");
        assertThat((BigDecimal) row.get("delivery_net_kg")).isEqualByComparingTo(DISPATCH_NET);
    }

    @Test
    @DisplayName("Tamamlanan sevkiyat sonradan iptal edilmez ve durumu geriye gitmez")
    void shouldNotMoveCompletedShipmentBack() {
        UUID id = UUID.randomUUID();
        readModel.insertShipment(ShipmentFixtures.created(id, "SHP-C", SITE, SUBCONTRACTOR));

        assertThat(readModel.saveCompleted(ShipmentFixtures.completed(id))).isTrue();
        assertThat(readModel.saveCancelled(ShipmentFixtures.cancelled(id))).isFalse();
        assertThat(readModel.saveDispatched(ShipmentFixtures.dispatched(id, DISPATCH_NET)))
                .isTrue();

        assertThat(shipment(id)).containsEntry("status", "COMPLETED");
    }

    @Test
    @DisplayName("Olmayan sevkiyatın güncellemesi false döner")
    void shouldReportMissingShipment() {
        assertThat(readModel.saveCancelled(ShipmentFixtures.cancelled(UUID.randomUUID())))
                .isFalse();
    }

    private Map<String, @Nullable Object> shipment(UUID id) {
        return jdbc.queryForMap("SELECT * FROM shipment_view WHERE shipment_id = ?", id);
    }
}
