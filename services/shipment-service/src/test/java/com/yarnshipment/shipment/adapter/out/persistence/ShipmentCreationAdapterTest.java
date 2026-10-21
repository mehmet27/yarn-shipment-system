package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Taslak, durum geçmişi ve outbox olayı gerçek tetikleyiciler ve uygulama rolüyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ShipmentCreationAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class ShipmentCreationAdapterTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000c001");
    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentCreationAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Taslak numarasıyla yazılır; ilk geçmiş satırı önceki durumsuzdur ve olay outbox'a girer")
    void shouldCreateDraftWithHistoryAndOutbox() {
        ShipmentParties parties = new ShipmentParties(SUBCONTRACTOR, "Örnek Tekstil", null, null, null);

        CreatedShipment created = adapter.create(new NewShipment(SITE, parties, null, NOW), USER);

        assertThat(created.shipmentNo()).startsWith("SV-20261021-");
        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, created.id()))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject(
                        "SELECT from_status FROM shipment_status_history WHERE shipment_id = ?",
                        String.class,
                        created.id()))
                .isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        Long.class,
                        created.id(),
                        "com.yarnshipment.shipment.shipment-created.v1"))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Şoför ve araç anlık görüntüsüyle birlikte yazılır")
    void shouldStoreDriverAndVehicleSnapshots() {
        UUID driver = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();
        ShipmentParties parties = new ShipmentParties(SUBCONTRACTOR, "Örnek Tekstil", driver, "Ali Yılmaz", "34ABC123");

        CreatedShipment created = adapter.create(new NewShipment(SITE, parties, vehicle, NOW), USER);

        assertThat(jdbc.queryForObject("SELECT plate_no FROM shipment WHERE id = ?", String.class, created.id()))
                .isEqualTo("34ABC123");
        assertThat(jdbc.queryForObject("SELECT vehicle_id FROM shipment WHERE id = ?", UUID.class, created.id()))
                .isEqualTo(vehicle);
    }
}
