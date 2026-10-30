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
import com.yarnshipment.discrepancy.application.port.in.claim.RecordInspectionResultUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.RequestInspectionUseCase;
import com.yarnshipment.discrepancy.application.port.in.claim.ShipmentClaims;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Kalite itirazı uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = QualityClaimController.class)
class QualityClaimControllerTest {

    private static final String CLAIMS = "/api/v1/quality-claims";
    private static final String INSPECTIONS = CLAIMS + "/" + TestClaims.CLAIM_ID + "/inspections";
    private static final String OPEN_BODY = "{\"shipmentId\":\"" + TestDiscrepancies.SHIPMENT
            + "\",\"description\":\"B kalite\",\"bagKeys\":[\"UK-0002\"],\"evidenceIds\":[\"" + TestClaims.PHOTO
            + "\"]}";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetShipmentClaimsUseCase getShipmentClaims;

    @MockitoBean
    private OpenQualityClaimUseCase openQualityClaim;

    @MockitoBean
    private RequestInspectionUseCase requestInspection;

    @MockitoBean
    private RecordInspectionResultUseCase recordInspectionResult;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    QualityClaimControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Fasoncu itiraz açar ve 201 alır; süre dolmuşsa QUALITY_CLAIM_WINDOW_CLOSED ile 409 alır")
    void shouldOpenClaim() {
        when(openQualityClaim.openQualityClaim(any())).thenReturn(file());

        MvcTestResult created = post(CLAIMS, OPEN_BODY, TestTokens.subcontractorToken());
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.discrepancyNo").isEqualTo("UY-SV-3-1");
        assertThat(created)
                .bodyJson()
                .extractingPath("$.allowedActions")
                .asArray()
                .isEmpty();

        when(openQualityClaim.openQualityClaim(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.WINDOW_CLOSED, "test"));
        MvcTestResult late = post(CLAIMS, OPEN_BODY, TestTokens.subcontractorToken());
        assertThat(late).hasStatus(HttpStatus.CONFLICT);
        assertThat(late).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_WINDOW_CLOSED");
    }

    @Test
    @DisplayName("Fotoğrafsız ya da çuvalsız istek 400 alır; kanıt kuralı QUALITY_CLAIM_EVIDENCE_INVALID ile 400 alır")
    void shouldValidateOpenRequest() {
        String noPhoto = OPEN_BODY.replace("\"" + TestClaims.PHOTO + "\"", "");
        when(openQualityClaim.openQualityClaim(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.EVIDENCE_INVALID, "test"));

        assertThat(post(CLAIMS, noPhoto, TestTokens.subcontractorToken())).hasStatus(HttpStatus.BAD_REQUEST);
        MvcTestResult invalid = post(CLAIMS, OPEN_BODY, TestTokens.subcontractorToken());
        assertThat(invalid).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalid).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_EVIDENCE_INVALID");
    }

    @Test
    @DisplayName("İtiraz dosyası açık süre ve izinle OPEN_CLAIM eylemini taşır")
    void shouldReturnShipmentClaims() {
        when(getShipmentClaims.getShipmentClaims(any()))
                .thenReturn(new ShipmentClaims(TestClaims.target(), true, List.of(file())));

        MvcTestResult result = mvc.get()
                .uri(CLAIMS + "/shipments/" + TestDiscrepancies.SHIPMENT)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.subcontractorToken())))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.allowedActions[0]").isEqualTo("OPEN_CLAIM");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.deliveredKeys")
                .asArray()
                .hasSize(2);
    }

    @Test
    @DisplayName("Kalite kontrol ister ve sonuç girer; sonucu girilmiş kontrol 409 alır")
    void shouldRequestAndRecordInspection() {
        when(requestInspection.requestInspection(any())).thenReturn(TestClaims.pending(InspectionKind.LAB));
        when(recordInspectionResult.recordInspectionResult(any()))
                .thenThrow(new ClaimRejectedException(ClaimRejection.INSPECTION_RECORDED, "test"));

        MvcTestResult requested = post(INSPECTIONS, "{\"kind\":\"LAB\"}", quality());
        assertThat(requested).hasStatus(HttpStatus.CREATED);
        assertThat(requested).bodyJson().extractingPath("$.allowedActions[0]").isEqualTo("RECORD_RESULT");

        MvcTestResult recorded =
                post(INSPECTIONS + "/" + TestClaims.INSPECTION_ID + "/result", "{\"result\":\"aynı\"}", quality());
        assertThat(recorded).hasStatus(HttpStatus.CONFLICT);
        assertThat(recorded).bodyJson().extractingPath("$.code").isEqualTo("QUALITY_CLAIM_INSPECTION_RECORDED");
    }

    @Test
    @DisplayName("Fasoncu, depo ve yönetici kontrol isteyemez; depo ve yönetici itiraz açamaz")
    void shouldCheckPermissions() {
        Jwt manager = TestTokens.token("manager").build();
        Jwt warehouse = TestTokens.token("warehouse").build();

        assertThat(post(INSPECTIONS, "{\"kind\":\"SAMPLE\"}", TestTokens.subcontractorToken()))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(INSPECTIONS, "{\"kind\":\"SAMPLE\"}", warehouse)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(INSPECTIONS, "{\"kind\":\"SAMPLE\"}", manager)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(CLAIMS, OPEN_BODY, warehouse)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(CLAIMS, OPEN_BODY, manager)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(CLAIMS, OPEN_BODY, TestTokens.driverToken())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private static ClaimFile file() {
        return new ClaimFile(
                TestClaims.claim(List.of("UK-0002")), TestDiscrepancies.open(TestTokens.SITE_ID), List.of());
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
