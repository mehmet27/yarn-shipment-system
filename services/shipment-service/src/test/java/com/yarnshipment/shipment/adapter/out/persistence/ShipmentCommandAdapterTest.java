package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.arrival.ArrivalDeclaration;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.PickupConfirmation;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Yazma adapter'ı gerçek tetikleyiciler ve uygulama rolünün yetkileriyle çalışır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ShipmentCommandAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(JacksonAutoConfiguration.class)
class ShipmentCommandAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID DRIVER = UUID.fromString("0192f0a0-0000-7000-8000-00000000e001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final int BAG_COUNT = 40;
    private static final PickupConfirmation PICKUP =
            new PickupConfirmation(SHIPMENT, SITE, DRIVER, BAG_COUNT, "M-0001", Instant.parse("2026-10-21T09:00:00Z"));
    private static final ArrivalDeclaration ARRIVAL = new ArrivalDeclaration(
            SHIPMENT, SITE, DRIVER, BAG_COUNT, "M-0001", false, Instant.parse("2026-10-21T13:00:00Z"));
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id,
                                  driver_name, status, created_at, created_by)
            VALUES (?, ?, 'SV-1', ?, 'Örnek Tekstil', ?, 'Ali Yılmaz', 'LOADED', now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentCommandAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertLoadedShipment() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, UUID.randomUUID(), DRIVER, USER);
    }

    @Test
    @DisplayName("Beyan durumu IN_TRANSIT yapar; beyan, geçmiş ve outbox olayı aynı transaction'da yazılır")
    void shouldRecordPickupWithHistoryAndOutbox() {
        // when
        adapter.recordPickup(PICKUP, USER);

        // then
        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("IN_TRANSIT");
        assertThat(adapter.hasPickupConfirmation(SHIPMENT)).isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT to_status FROM shipment_status_history WHERE shipment_id = ?", String.class, SHIPMENT))
                .isEqualTo("IN_TRANSIT");
        assertThat(jdbc.queryForObject(
                        "SELECT payload ->> 'bagCount' FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        String.class,
                        SHIPMENT,
                        "com.yarnshipment.shipment.shipment-dispatched.v1"))
                .isEqualTo(String.valueOf(BAG_COUNT));
    }

    @Test
    @DisplayName("Durum arada değiştiyse beyan yazılmaz ve 409 hatası verilir")
    void shouldRejectWhenStatusChangedConcurrently() {
        // given
        adapter.recordPickup(PICKUP, USER);

        // when / then
        assertThatThrownBy(() -> adapter.recordPickup(PICKUP, USER)).isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Beyanı olmayan sevkiyatta tekrar kontrolü yanlış döner")
    void shouldReportMissingPickup() {
        assertThat(adapter.hasPickupConfirmation(SHIPMENT)).isFalse();
    }

    @Test
    @DisplayName("Varış beyanı durumu AWAITING_DELIVERY yapar; mühür durumu ve outbox olayı yazılır")
    void shouldRecordArrivalWithSealConditionAndOutbox() {
        // given
        adapter.recordPickup(PICKUP, USER);

        // when
        adapter.recordArrival(ARRIVAL, USER);

        // then
        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("AWAITING_DELIVERY");
        assertThat(adapter.hasArrivalDeclaration(SHIPMENT)).isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT seal_intact FROM driver_declaration WHERE shipment_id = ? AND declaration_type = ?",
                        Boolean.class,
                        SHIPMENT,
                        "ARRIVAL"))
                .isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT payload ->> 'sealIntact' FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        String.class,
                        SHIPMENT,
                        "com.yarnshipment.shipment.shipment-arrived.v1"))
                .isEqualTo("false");
    }

    @Test
    @DisplayName("Yolda olmayan sevkiyatta varış yazılmaz ve 409 hatası verilir")
    void shouldRejectArrivalBeforePickup() {
        assertThatThrownBy(() -> adapter.recordArrival(ARRIVAL, USER))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(adapter.hasArrivalDeclaration(SHIPMENT)).isFalse();
    }
}
