package com.yarnshipment.shipment.masterdata.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ChangeLotStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.CreateLotUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.LockLotQualityUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.UpdateLotUseCase;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.LotQualityLockedException;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Parti ve kalite kilidi uçlarının sözleşmesi: LOT_QUALITY_LOCK izni, istek doğrulama ve hata eşlemesi. */
@WebMvcTest(controllers = LotManagementController.class)
class LotManagementControllerTest {

    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final UUID LOT = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    private static final UUID PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final UUID GRADE = UUID.fromString("0192f0a0-0000-7000-8000-000000000211");
    private static final String URI = "/api/v1/master-data/lots";
    private static final String BODY = "{\"siteId\":\"" + TestTokens.SITE_ID + "\",\"lotNo\":\"p-1\",\"productId\":\""
            + PRODUCT + "\",\"qualityGradeId\":\"" + GRADE + "\",\"productionDate\":\"2026-10-10\"}";
    private static final Instant LOCKED_AT = Instant.parse("2026-10-11T08:00:00Z");

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateLotUseCase create;

    @MockitoBean
    private UpdateLotUseCase update;

    @MockitoBean
    private ChangeLotStatusUseCase status;

    @MockitoBean
    private LockLotQualityUseCase lock;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    LotManagementControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Kalite kullanıcısı parti açar; 201 ve kilitsiz parti döner")
    void shouldCreateLotAsQuality() {
        // given
        when(create.createLot(any(), eq(TestTokens.SITE_ID), any())).thenReturn(lot(null));

        // when
        MvcTestResult result = exchange(mvc.post().uri(URI).header("Idempotency-Key", KEY), "quality", BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.lotNo").isEqualTo("P-1");
        assertThat(result).bodyJson().doesNotHavePath("$.qualityLockedAt");
    }

    @Test
    @DisplayName("Depo kullanıcısı parti açamaz ve kilitleyemez")
    void shouldForbidWarehouse() {
        assertThat(exchange(mvc.post().uri(URI).header("Idempotency-Key", KEY), "warehouse", BODY))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(exchange(mvc.post().uri(URI + "/" + LOT + "/quality-lock"), "warehouse", null))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(create, lock);
    }

    @Test
    @DisplayName("Eksik alan şemadan 400 alır")
    void shouldRejectMissingField() {
        String withoutDate = BODY.replace(",\"productionDate\":\"2026-10-10\"", "");

        assertThat(exchange(mvc.post().uri(URI).header("Idempotency-Key", KEY), "admin", withoutDate))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(create);
    }

    @Test
    @DisplayName("Kilit 200 ve kilit anını döner; kilitli parti LOT_QUALITY_LOCKED ile 409")
    void shouldLockAndMapLocked() {
        // given
        UUID locked = UUID.randomUUID();
        when(lock.lockLotQuality(any(), eq(LOT))).thenReturn(lot(LOCKED_AT));
        when(lock.lockLotQuality(any(), eq(locked))).thenThrow(new LotQualityLockedException(locked));

        // when
        MvcTestResult ok = exchange(mvc.post().uri(URI + "/" + LOT + "/quality-lock"), "admin", null);
        MvcTestResult conflict = exchange(mvc.post().uri(URI + "/" + locked + "/quality-lock"), "admin", null);

        // then
        assertThat(ok).hasStatusOk();
        assertThat(ok).bodyJson().extractingPath("$.qualityLockedAt").isEqualTo("2026-10-11T08:00:00Z");
        assertThat(conflict).hasStatus(HttpStatus.CONFLICT);
        assertThat(conflict).bodyJson().extractingPath("$.code").isEqualTo("LOT_QUALITY_LOCKED");
    }

    private static ProductionLot lot(@Nullable Instant lockedAt) {
        return new ProductionLot(
                LOT,
                TestTokens.SITE_ID,
                new LotDetails("P-1", PRODUCT, GRADE, LocalDate.parse("2026-10-10")),
                new ProductionLot.Labels("Merkez Fabrika", "PES-30-1", "Polyester 30/1", "A", "Birinci kalite"),
                lockedAt,
                true);
    }

    private MvcTestResult exchange(MockMvcTester.MockMvcRequestBuilder builder, String role, @Nullable String body) {
        if (body != null) {
            builder.contentType("application/json").content(body);
        }
        return builder.with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .exchange();
    }
}
