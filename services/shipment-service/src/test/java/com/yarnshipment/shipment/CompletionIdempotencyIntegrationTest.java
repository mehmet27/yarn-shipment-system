package com.yarnshipment.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import com.yarnshipment.shipment.application.port.out.EvidenceLookupPort;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
class CompletionIdempotencyIntegrationTest {

    /** Bu test sevkiyatı commit eder; ortak veritabanındaki sayım testlerini bozmaması için kendi konteyneri vardır. */
    private static final PostgresTestDatabase ISOLATED_DATABASE = PostgresTestDatabase.startIsolated();

    private static final int SHORT_ID = 8;
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, ?, ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', now(), ?)""";
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000000ee");

    /** Uygulama rolü satır silemez (DML yalnızca ekleme/güncelleme); her test kendi sevkiyatını açar. */
    private final UUID shipment = UUID.randomUUID();

    private final MockMvcTester mvc;
    private final JdbcTemplate jdbc;
    private final Authentication manager =
            TestTokens.CONVERTER.convert(TestTokens.token("manager").build());

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private EvidenceLookupPort evidence;

    @Autowired
    CompletionIdempotencyIntegrationTest(MockMvcTester mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        ISOLATED_DATABASE.registerIn(registry);
    }

    @BeforeEach
    void insertAwaitingShipment() {
        jdbc.update("DELETE FROM idempotency_record");
        jdbc.update(
                INSERT_SHIPMENT,
                shipment,
                TestTokens.SITE_ID,
                "SV-" + shipment.toString().substring(0, SHORT_ID),
                TestTokens.SUBCONTRACTOR_ID,
                UUID.randomUUID());
        when(evidence.findEvidence(any()))
                .thenReturn(Optional.of(
                        new EvidenceReference(EVIDENCE, shipment, EvidenceReference.WAYBILL, EvidenceReference.VALID)));
    }

    private MvcTestResult complete() {
        return mvc.post()
                .uri("/api/v1/shipments/" + shipment + "/completion")
                .header("Idempotency-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"waybillNo\":\"IRS-1\",\"waybillEvidenceId\":\"" + EVIDENCE + "\"}")
                .with(SecurityMockMvcRequestPostProcessors.authentication(manager))
                .exchange();
    }

    @Test
    @DisplayName("Aynı Idempotency-Key ile tekrar gelen tamamlama ilk yanıtı döner ve geçmiş bir kez yazılır")
    void shouldReplayCompletionForSameKey() {
        // when
        MvcTestResult first = complete();
        MvcTestResult second = complete();

        // then

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
