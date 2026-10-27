package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadUseCase;
import com.yarnshipment.evidence.application.port.in.RequestUploadUseCase;
import com.yarnshipment.evidence.application.port.in.UploadTicket;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import java.util.Map;
import java.util.UUID;
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

/** Yükleme uçlarının sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = EvidenceUploadController.class)
class EvidenceUploadControllerTest {

    private static final String REQUEST_URI = "/api/v1/evidence/uploads";
    private static final String CONFIRM_URI = REQUEST_URI + "/" + TestUploads.EVIDENCE_ID + "/confirm";
    private static final String BODY = "{\"shipmentId\":\"" + TestUploads.SHIPMENT_ID + "\","
            + "\"evidenceType\":\"DISPATCH_SCALE\",\"mimeType\":\"image/jpeg\",\"sizeBytes\":2048,"
            + "\"source\":\"CAMERA\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private RequestUploadUseCase requestUpload;

    @MockitoBean
    private ConfirmUploadUseCase confirmUpload;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    EvidenceUploadControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("İstek 201 ile imzalı adresi ve başlıkları döner")
    void shouldReturnTicket() {
        when(requestUpload.requestUpload(any()))
                .thenReturn(new UploadTicket(
                        TestUploads.EVIDENCE_ID,
                        "https://s3/put",
                        Map.of("content-type", "image/jpeg"),
                        TestUploads.expiry()));

        MvcTestResult result = post(REQUEST_URI, BODY, warehouse());

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.uploadUrl").isEqualTo("https://s3/put");
        assertThat(result).bodyJson().extractingPath("$.headers.content-type").isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Onay kanıtı sunucunun hesapladığı hash ile döner")
    void shouldConfirm() {
        UUID user = UUID.randomUUID();
        when(confirmUpload.confirmUpload(any()))
                .thenReturn(EvidenceFile.confirm(TestUploads.intent(user), TestUploads.stored(), TestUploads.NOW));

        MvcTestResult result = post(CONFIRM_URI, "", warehouse());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.sha256").isEqualTo(TestUploads.SHA);
        assertThat(result).bodyJson().extractingPath("$.evidenceType").isEqualTo("DISPATCH_SCALE");
    }

    @Test
    @DisplayName("Yükleme izni olmayan roller 403, token'sız istek 401 alır; bilinmeyen tür 400 alır")
    void shouldCheckPermissionsAndBody() {
        assertThat(post(REQUEST_URI, BODY, TestTokens.token("manager").build())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri(REQUEST_URI).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(post(REQUEST_URI, BODY.replace("DISPATCH_SCALE", "SELFIE"), warehouse()))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    private static Jwt warehouse() {
        return TestTokens.token("warehouse").build();
    }

    private MvcTestResult post(String uri, String body, Jwt jwt) {
        var request = mvc.post()
                .uri(uri)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf());
        if (!body.isEmpty()) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return request.exchange();
    }
}
