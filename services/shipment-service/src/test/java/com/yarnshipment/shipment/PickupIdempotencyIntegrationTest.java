package com.yarnshipment.shipment;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Tüm uygulama zinciri gerçek PostgreSQL ile: aynı Idempotency-Key tekrarında ilk yanıt döner, iş bir kez yapılır. */
@SpringBootTest
@AutoConfigureMockMvc
class PickupIdempotencyIntegrationTest {

    /** Bu test sevkiyatı commit eder; ortak veritabanındaki sayım testlerini bozmaması için kendi konteyneri vardır. */
    private static final PostgresTestDatabase ISOLATED_DATABASE = PostgresTestDatabase.startIsolated();

    private static final int SHORT_ID = 8;
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id,
                                  driver_name, status, created_at, created_by)
            VALUES (?, ?, ?, ?, 'Örnek Tekstil', ?, 'Ali Yılmaz', 'LOADED', now(), ?)""";

    /** Uygulama rolü satır silemez (DML yalnızca ekleme/güncelleme); her test kendi sevkiyatını açar. */
    private final UUID shipment = UUID.randomUUID();

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final Authentication driver = TestTokens.CONVERTER.convert(TestTokens.driverToken());

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    PickupIdempotencyIntegrationTest(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        ISOLATED_DATABASE.registerIn(registry);
        registry.add("yarnshipment.outbox.enabled", () -> "false");
    }

    @BeforeEach
    void insertLoadedShipment() {
        jdbc.update("DELETE FROM idempotency_record");
        jdbc.update(
                INSERT_SHIPMENT,
                shipment,
                TestTokens.SITE_ID,
                "SV-" + shipment.toString().substring(0, SHORT_ID),
                TestTokens.SUBCONTRACTOR_ID,
                TestTokens.DRIVER_ID,
                UUID.randomUUID());
    }

    private MvcTestResult pickup() {
        return mvc.post()
                .uri("/api/v1/shipments/" + shipment + "/pickup-confirmation")
                .header("Idempotency-Key", KEY)
                .with(SecurityMockMvcRequestPostProcessors.authentication(driver))
                .exchange();
    }

    @Test
    @DisplayName("Aynı Idempotency-Key ile tekrar gelen beyan ilk yanıtı döner ve geçmiş bir kez yazılır")
    void shouldReplayPickupForSameKey() {
        MvcTestResult first = pickup();
        MvcTestResult second = pickup();

        assertThat(first).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(second).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(second.getResponse().getHeader("Idempotency-Replayed")).isEqualTo("true");
        assertThat(first.getResponse().getHeader("X-Request-Id")).isNotBlank();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM shipment_status_history WHERE shipment_id = ?", Integer.class, shipment))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT state FROM idempotency_record", String.class))
                .isEqualTo("COMPLETED");
    }
}
