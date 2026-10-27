package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.check.CheckConclusion;
import com.yarnshipment.shipment.domain.check.ShipmentCompleted;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
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

/** Kontrol sonucu gerçek durum tetikleyicisi ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CheckConclusionAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class CheckConclusionAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000401");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-4', ?, 'Örnek Tekstil', 'CHECKING', now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private CheckConclusionAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertCheckingShipment() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, UUID.randomUUID(), USER);
    }

    @Test
    @DisplayName("Temiz sonuç sevkiyatı tamamlar; geçmiş sistem hesabıyla, tamamlanma olayı outbox'a yazılır")
    void shouldCompleteShipment() {
        adapter.recordConclusion(
                new CheckConclusion(SHIPMENT, SITE, ShipmentStatus.CHECKING, ShipmentStatus.COMPLETED, NOW));

        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
                        "SELECT changed_by FROM shipment_status_history WHERE shipment_id = ?", UUID.class, SHIPMENT))
                .isEqualTo(CheckConclusion.SYSTEM_ACTOR);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                        Integer.class,
                        SHIPMENT,
                        ShipmentCompleted.TYPE))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Uyuşmazlıkta sevkiyat incelemeye alınır ve olay yayınlanmaz")
    void shouldMoveToReview() {
        adapter.recordConclusion(
                new CheckConclusion(SHIPMENT, SITE, ShipmentStatus.CHECKING, ShipmentStatus.IN_REVIEW, NOW));

        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("IN_REVIEW");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE aggregate_id = ?", Integer.class, SHIPMENT))
                .isZero();
    }

    @Test
    @DisplayName("Durum arada değiştiyse ikinci sonuç reddedilir")
    void shouldRejectSecondConclusion() {
        CheckConclusion conclusion =
                new CheckConclusion(SHIPMENT, SITE, ShipmentStatus.CHECKING, ShipmentStatus.IN_REVIEW, NOW);
        adapter.recordConclusion(conclusion);

        assertThatThrownBy(() -> adapter.recordConclusion(conclusion))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }
}
