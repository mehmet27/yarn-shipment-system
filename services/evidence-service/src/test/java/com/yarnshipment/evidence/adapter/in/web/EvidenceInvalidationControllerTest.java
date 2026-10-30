package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.InvalidateEvidenceUseCase;
import com.yarnshipment.evidence.domain.model.EvidenceAlreadyInvalidatedException;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.InvalidationReasonRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Geçersiz kılma ucunun sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = EvidenceInvalidationController.class)
class EvidenceInvalidationControllerTest {

    private static final String URI = "/api/v1/evidence/" + TestUploads.EVIDENCE_ID + "/invalidate";
    private static final String BODY = "{\"reason\":\"yanlış sevkiyat\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private InvalidateEvidenceUseCase invalidateEvidence;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    EvidenceInvalidationControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Admin geçersiz kılar; cevap INVALIDATED durumu ve gerekçeyi taşır")
    void shouldInvalidate() {
        when(invalidateEvidence.invalidateEvidence(any()))
                .thenReturn(TestUploads.file(EvidenceType.SEAL).invalidate("yanlış sevkiyat"));

        MvcTestResult result = post("admin", BODY);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("INVALIDATED");
        assertThat(result).bodyJson().extractingPath("$.invalidatedReason").isEqualTo("yanlış sevkiyat");
    }

    @Test
    @DisplayName("Tekrar 409, gerekçesiz 400, admin olmayan 403 alır")
    void shouldMapFailures() {
        when(invalidateEvidence.invalidateEvidence(any()))
                .thenThrow(new EvidenceAlreadyInvalidatedException(TestUploads.EVIDENCE_ID));
        MvcTestResult repeat = post("admin", BODY);
        assertThat(repeat).hasStatus(HttpStatus.CONFLICT);
        assertThat(repeat).bodyJson().extractingPath("$.code").isEqualTo("EVIDENCE_ALREADY_INVALIDATED");

        doThrow(new InvalidationReasonRequiredException())
                .when(invalidateEvidence)
                .invalidateEvidence(any());
        MvcTestResult blank = post("admin", "{\"reason\":\" \"}");
        assertThat(blank).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(blank).bodyJson().extractingPath("$.code").isEqualTo("INVALIDATION_REASON_REQUIRED");

        assertThat(post("warehouse", BODY)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post("admin", "{}")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MvcTestResult post(String role, String body) {
        return mvc.post()
                .uri(URI)
                .header("Idempotency-Key", "0192f0a0-0000-7000-8000-0000000000dd")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
