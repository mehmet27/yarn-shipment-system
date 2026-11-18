package com.yarnshipment.discrepancy.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.TestClaims;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.claim.ClaimFile;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.OpenQualityClaimUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.ShipmentClaims;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Kalite itirazı uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = QualityClaimController.class)
class QualityClaimControllerTest {

    private static final String CLAIMS = "/api/v1/quality-claims";
    private static final String OPEN_BODY = "{\"shipmentId\":\"" + TestDiscrepancies.SHIPMENT
            + "\",\"description\":\"B kalite\",\"claimedKg\":\"120.5\",\"evidenceIds\":[\"" + TestClaims.PHOTO
            + "\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetShipmentClaimsUseCase getShipmentClaims;

    @MockitoBean
    private OpenQualityClaimUseCase openQualityClaim;

    @Autowired
    QualityClaimControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Kalite itiraz açar ve 201 alır; süre dolmuşsa QUALITY_CLAIM_WINDOW_CLOSED ile 409 alır")
    void shouldOpenClaim() {
        when(openQualityClaim.openQualityClaim(any())).thenReturn(file());

        MvcTestResult created = post(CLAIMS, OPEN_BODY, quality());
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.discrepancyNo").isEqualTo("UY-SV-3-1");
        assertThat(created).bodyJson().extractingPath("$.claimedKg").isEqualTo("120.500");
        assertThat(created).bodyJson().doesNotHavePath("$.bagKeys");
        assertThat(created).bodyJson().doesNotHavePath("$.inspections");

        when(openQualityClaim.openQualityClaim(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.WINDOW_CLOSED, "test"));
        MvcTestResult late = post(CLAIMS, OPEN_BODY, quality());
        assertThat(late).hasStatus(HttpStatus.CONFLICT);
        assertThat(late).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_WINDOW_CLOSED");
    }

    @Test
    @DisplayName("Boş açıklama QUALITY_CLAIM_DESCRIPTION_REQUIRED ile 400 alır")
    void shouldRejectBlankDescription() {
        when(openQualityClaim.openQualityClaim(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.DESCRIPTION_REQUIRED, "test"));

        MvcTestResult result = post(CLAIMS, OPEN_BODY, quality());

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_DESCRIPTION_REQUIRED");
    }

    @Test
    @DisplayName("Fotoğrafsız ya da kilosuz istek 400 alır; kanıt kuralı QUALITY_CLAIM_EVIDENCE_INVALID ile 400 alır")
    void shouldValidateOpenRequest() {
        String noPhoto = OPEN_BODY.replace("\"" + TestClaims.PHOTO + "\"", "");
        when(openQualityClaim.openQualityClaim(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.EVIDENCE_INVALID, "test"));

        assertThat(post(CLAIMS, noPhoto, quality())).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(CLAIMS, OPEN_BODY.replace("\"claimedKg\":\"120.5\",", ""), quality()))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(CLAIMS, OPEN_BODY.replace("120.5", "-1"), quality())).hasStatus(HttpStatus.BAD_REQUEST);
        MvcTestResult invalid = post(CLAIMS, OPEN_BODY, quality());
        assertThat(invalid).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalid).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_EVIDENCE_INVALID");
    }

    @Test
    @DisplayName("Sevkiyat kilosunu aşan itiraz QUALITY_CLAIM_CLAIM_EXCEEDS_SHIPMENT_WEIGHT ile 400 alır")
    void shouldRejectClaimAboveShipmentWeight() {
        when(openQualityClaim.openQualityClaim(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.CLAIM_EXCEEDS_SHIPMENT_WEIGHT, "test"));

        MvcTestResult result = post(CLAIMS, OPEN_BODY, quality());

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_CLAIM_EXCEEDS_SHIPMENT_WEIGHT");
    }

    @Test
    @DisplayName("İtiraz dosyası açık süre ve izinle OPEN_CLAIM eylemini taşır")
    void shouldReturnShipmentClaims() {
        when(getShipmentClaims.getShipmentClaims(any()))
                .thenReturn(new ShipmentClaims(TestClaims.target(), true, List.of(file())));

        MvcTestResult result = mvc.get()
                .uri(CLAIMS + "/shipments/" + TestDiscrepancies.SHIPMENT)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(quality())))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.allowedActions[0]").isEqualTo("OPEN_CLAIM");
        assertThat(result).bodyJson().extractingPath("$.receivedKg").isEqualTo("2000.000");
        assertThat(result).bodyJson().extractingPath("$.claimableKg").isEqualTo("2000.000");
        assertThat(result).bodyJson().doesNotHavePath("$.deliveredKeys");
        assertThat(result).bodyJson().extractingPath("$.claims[0].claimedKg").isEqualTo("120.500");
    }

    @Test
    @DisplayName("Depo ve müdür itiraz açamaz")
    void shouldCheckPermissions() {
        Jwt manager = manager();
        Jwt warehouse = TestTokens.token("warehouse").build();

        assertThat(post(CLAIMS, OPEN_BODY, warehouse)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(CLAIMS, OPEN_BODY, manager)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(CLAIMS, OPEN_BODY, TestTokens.unknownRoleToken())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private static ClaimFile file() {
        return new ClaimFile(TestClaims.claim(), TestDiscrepancies.open(TestTokens.SITE_ID));
    }

    private static Jwt manager() {
        return TestTokens.token("manager").build();
    }

    private static Jwt quality() {
        return TestTokens.token("quality").build();
    }

    private MvcTestResult post(String uri, String body, Jwt jwt) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
