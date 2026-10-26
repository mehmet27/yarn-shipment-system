package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestDeliveries;
import com.yarnshipment.shipment.domain.model.DeliveryReceipt;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentSide;
import com.yarnshipment.shipment.domain.model.Weighing;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

/** Teslim tutanağı gerçek tetikleyiciler ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DeliveryCommandAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class DeliveryCommandAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");
    private static final int BAGS = 40;
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-2', ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', now(), ?)""";
    private static final String INSERT_BAG = """
            INSERT INTO shipment_bag (id, shipment_id, bag_id, unique_key, lot_no, quality_grade_code, scan_method,
                                      assigned_at, assigned_by, created_at, created_by)
            VALUES (?, ?, ?, ?, 'LOT-1', 'A', 'SCAN', now(), ?, now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DeliveryCommandAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertAwaitingShipment() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, UUID.randomUUID(), USER);
        jdbc.update(INSERT_BAG, UUID.randomUUID(), SHIPMENT, UUID.randomUUID(), "UK-0001", USER, USER);
        jdbc.update(INSERT_BAG, UUID.randomUUID(), SHIPMENT, UUID.randomUUID(), "UK-0002", USER, USER);
    }

    @Test
    @DisplayName("Tutanak kilitli yazılır; okutmalar eşleşmeyle, teslim tartısı, mühür, geçmiş ve olay kaydedilir")
    void shouldRecordDelivery() {
        // when
        adapter.recordDelivery(receipt());

        // then
        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("CHECKING");
        assertThat(adapter.hasDeliveryReceipt(SHIPMENT)).isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT submitted_at IS NOT NULL FROM delivery_receipt WHERE shipment_id = ?",
                        Boolean.class,
                        SHIPMENT))
                .isTrue();
        assertThat(jdbc.queryForList(
                        "SELECT scanned_key || ':' || matched FROM delivery_scan ORDER BY scanned_key", String.class))
                .containsExactly("UK-0001:true", "UK-9999:false");
        assertThat(jdbc.queryForObject(
                        "SELECT net_kg::text FROM weighing WHERE shipment_id = ? AND side = 'DELIVERY'",
                        String.class,
                        SHIPMENT))
                .isEqualTo("1977.250");
        assertThat(jdbc.queryForObject(
                        "SELECT payload -> 'expectedUniqueKeys' ->> 1 FROM outbox_event WHERE aggregate_id = ?",
                        String.class,
                        SHIPMENT))
                .isEqualTo("UK-0002");
    }

    @Test
    @DisplayName("Tutanak ikinci kez yazılamaz; durum artık teslim beklemiyor")
    void shouldRejectSecondDelivery() {
        // given
        adapter.recordDelivery(receipt());

        // when / then
        assertThatThrownBy(() -> adapter.recordDelivery(receipt())).isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Tutanağı olmayan sevkiyatta tekrar kontrolü yanlış döner")
    void shouldReportMissingReceipt() {
        assertThat(adapter.hasDeliveryReceipt(SHIPMENT)).isFalse();
    }

    private static DeliveryReceipt receipt() {
        var measurement = TestDeliveries.measurement();
        Weighing weighing = new Weighing(measurement.grossKg(), measurement.tareKg(), BAGS, NOW);
        return new DeliveryReceipt(SHIPMENT, SITE, "SV-2", USER, NOW, weighing, measurement, ShipmentSide.EMPTY);
    }
}
