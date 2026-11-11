package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

/** Sevkiyat izleme ve kanıt türleri; gerçek şemada, uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ShipmentWatchAdapter.class, ShipmentEvidenceAdapter.class, DiscrepancyWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class ShipmentWatchAdaptersTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final Duration LIMIT = Duration.ofHours(24);
    private static final Instant DISPATCHED = NOW.minus(Duration.ofHours(30));
    private static final int PAGE = 10;
    private static final int UPLOADS = 3;
    private static final WatchedShipment SHIPMENT = new WatchedShipment(
            TestComparisons.SHIPMENT,
            TestComparisons.SHIPMENT_NO,
            TestComparisons.SITE,
            TestComparisons.SUBCONTRACTOR,
            null);

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentWatchAdapter watch;

    @Autowired
    private ShipmentEvidenceAdapter evidence;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Oluşturma ikinci kez yazılmaz; ilk yola çıkış zamanı kalır; yolda olan sevkiyat bir kez işaretlenir")
    void shouldTrackShipmentInTransit() {
        watch.recordCreated(SHIPMENT, NOW.minus(Duration.ofDays(2)));
        watch.recordCreated(new WatchedShipment(SHIPMENT.shipmentId(), "SV-X", SHIPMENT.siteId(), null, null), NOW);
        watch.recordDispatched(SHIPMENT.shipmentId(), DISPATCHED, TestComparisons.DISPATCH_KG);
        watch.recordDispatched(SHIPMENT.shipmentId(), NOW, null);

        List<WatchedShipment> overdue = watch.findInTransitSince(NOW.minus(LIMIT), PAGE);

        assertThat(overdue).singleElement().satisfies(found -> {
            assertThat(found.shipmentNo()).isEqualTo(TestComparisons.SHIPMENT_NO);
            assertThat(found.dispatchedAt()).isEqualTo(DISPATCHED);
        });
        assertThat(watch.findInTransitSince(DISPATCHED.minusSeconds(1), PAGE)).isEmpty();
        assertThat(watch.markTransitAlerted(SHIPMENT.shipmentId(), NOW)).isTrue();
        assertThat(watch.markTransitAlerted(SHIPMENT.shipmentId(), NOW)).isFalse();
        assertThat(watch.findInTransitSince(NOW, PAGE)).isEmpty();
    }

    @Test
    @DisplayName("Varan ya da kapanan sevkiyat yolda sayılmaz; izlenmeyen sevkiyatın adımı yok sayılır")
    void shouldExcludeArrivedAndClosed() {
        watch.recordCreated(SHIPMENT, NOW.minus(Duration.ofDays(2)));
        watch.recordDispatched(SHIPMENT.shipmentId(), DISPATCHED, TestComparisons.DISPATCH_KG);
        watch.recordClosed(SHIPMENT.shipmentId(), NOW);
        watch.recordDispatched(UUID.randomUUID(), DISPATCHED, null);

        assertThat(watch.findInTransitSince(NOW, PAGE)).isEmpty();
        assertThat(watch.findShipment(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Tamamlanma ve çıkış kilosu yazılır; ilk değer kalır, tamamlanan sevkiyat kapanır")
    void shouldRecordCompletionAndNetKg() {
        watch.recordCreated(SHIPMENT, NOW.minus(Duration.ofDays(2)));
        watch.recordDispatched(SHIPMENT.shipmentId(), DISPATCHED, TestComparisons.DISPATCH_KG);
        watch.recordCompleted(SHIPMENT.shipmentId(), NOW);
        watch.recordCompleted(SHIPMENT.shipmentId(), NOW.plusSeconds(1));

        assertThat(jdbc.queryForObject(
                        "SELECT completed_at FROM shipment_watch WHERE shipment_id = ?",
                        java.sql.Timestamp.class,
                        SHIPMENT.shipmentId()))
                .isEqualTo(java.sql.Timestamp.from(NOW));
        assertThat(jdbc.queryForObject(
                        "SELECT dispatch_net_kg FROM shipment_watch WHERE shipment_id = ?",
                        java.math.BigDecimal.class,
                        SHIPMENT.shipmentId()))
                .isEqualByComparingTo(TestComparisons.DISPATCH_KG);
        assertThat(watch.findInTransitSince(NOW, PAGE)).isEmpty();
    }

    @Test
    @DisplayName("Aynı kanıt bir kez yazılır; sevkiyatın kanıt türleri tekil döner")
    void shouldRecordEvidenceTypes() {
        UUID first = UUID.randomUUID();
        evidence.recordUploaded(first, SHIPMENT.shipmentId(), "SEAL", NOW);
        evidence.recordUploaded(first, SHIPMENT.shipmentId(), "SEAL", NOW);
        evidence.recordUploaded(UUID.randomUUID(), SHIPMENT.shipmentId(), "SEAL", NOW);
        evidence.recordUploaded(UUID.randomUUID(), SHIPMENT.shipmentId(), "BAG_OVERVIEW", NOW);

        assertThat(evidence.findEvidenceTypes(SHIPMENT.shipmentId())).containsExactly("BAG_OVERVIEW", "SEAL");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shipment_evidence", Integer.class))
                .isEqualTo(UPLOADS);
    }
}
