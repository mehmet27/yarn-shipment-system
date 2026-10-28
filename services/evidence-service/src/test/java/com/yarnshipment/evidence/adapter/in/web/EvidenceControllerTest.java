package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import java.util.UUID;
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

/** Kanıt okuma ucunun sözleşmesi ve izin denetimi. */
@WebMvcTest(controllers = EvidenceController.class)
class EvidenceControllerTest {

    private static final String URI = "/api/v1/evidence/" + TestUploads.EVIDENCE_ID;

    private final MockMvcTester mvc;

    @MockitoBean
    private GetEvidenceUseCase getEvidence;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    EvidenceControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Kanıt üst verisi döner; yok ise EVIDENCE_NOT_FOUND ile 404")
    void shouldReturnOrHideEvidence() {
        when(getEvidence.getEvidence(any()))
                .thenReturn(EvidenceFile.confirm(
                        TestUploads.intent(UUID.randomUUID()), TestUploads.stored(), TestUploads.NOW));
        MvcTestResult found = get("warehouse");
        assertThat(found).hasStatusOk();
        assertThat(found).bodyJson().extractingPath("$.evidenceType").isEqualTo("DISPATCH_SCALE");

        when(getEvidence.getEvidence(any())).thenThrow(new EvidenceNotFoundException(TestUploads.EVIDENCE_ID));
        MvcTestResult missing = get("warehouse");
        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(missing).bodyJson().extractingPath("$.code").isEqualTo("EVIDENCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Görüntüleme izni olmayan 403, token'sız istek 401 alır")
    void shouldCheckPermission() {
        assertThat(get("nobody")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri(URI).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult get(String role) {
        return mvc.get()
                .uri(URI)
                .with(SecurityMockMvcRequestPostProcessors.authentication(
                        TestTokens.CONVERTER.convert(TestTokens.token(role).build())))
                .exchange();
    }
}
