package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.cancellation.Cancellation;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
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

/** İptal yazımı gerçek tetikleyiciler ve uygulama rolünün yetkileriyle çalışır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ShipmentCancellationAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(JacksonAutoConfiguration.class)
class ShipmentCancellationAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Cancellation CANCELLATION = new Cancellation(
            SHIPMENT, SITE, ShipmentStatus.DRAFT, "Yanlış sevkiyat", Instant.parse("2026-10-23T09:00:00Z"));
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-2', ?, 'Örnek Tekstil', 'DRAFT', now(), ?)""";
    private static final String INSERT_BAG = """
            INSERT INTO shipment_bag (id, shipment_id, bag_id, unique_key, lot_no, quality_grade_code, scan_method,
                                      assigned_at, assigned_by, created_at, created_by)
            VALUES (?, ?, ?, ?, 'P-1', 'A', 'SCAN', now(), ?, now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentCancellationAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertDraftShipmentWithBag() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, UUID.randomUUID(), USER);
        jdbc.update(INSERT_BAG, UUID.randomUUID(), SHIPMENT, UUID.randomUUID(), "BAG-0001", USER, USER);
    }

    @Test
    @DisplayName("İptal durumu, gerekçeyi, geçmişi ve olayı yazar; çuvalları çıkarılmış işaretler")
    void shouldCancelWithReasonHistoryEventAndBagRemoval() {
        adapter.cancel(CANCELLATION, USER);

        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancelled_reason FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("Yanlış sevkiyat");
        assertThat(jdbc.queryForObject(
                        "SELECT reason FROM shipment_status_history WHERE shipment_id = ? AND to_status = 'CANCELLED'",
                        String.class,
                        SHIPMENT))
                .isEqualTo("Yanlış sevkiyat");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM shipment_bag WHERE shipment_id = ? AND removed_at IS NOT NULL",
                        Long.class,
                        SHIPMENT))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        Long.class,
                        SHIPMENT,
                        "com.yarnshipment.shipment.shipment-cancelled.v1"))
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Durum arada değiştiyse iptal yazılmaz ve 409 hatası verilir")
    void shouldRejectWhenStatusChangedConcurrently() {
        adapter.cancel(CANCELLATION, USER);

        assertThatThrownBy(() -> adapter.cancel(CANCELLATION, USER)).isInstanceOf(InvalidShipmentStatusException.class);
    }
}
