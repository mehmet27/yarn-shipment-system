package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.ConfirmUploadUseCase;
import com.yarnshipment.evidence.application.port.in.RequestUploadUseCase;
import com.yarnshipment.evidence.domain.model.ShipmentLockedException;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import com.yarnshipment.evidence.domain.model.ShipmentServiceUnavailableException;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadMismatchException;
import com.yarnshipment.evidence.domain.model.UploadNotAllowedException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
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

/** Servise özgü hataların ERR-07 kodları ve durumları. */
@WebMvcTest(controllers = EvidenceUploadController.class)
class EvidenceUploadErrorsTest {

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
    EvidenceUploadErrorsTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Alan hataları ERR-07 kodlarına ve durumlarına çevrilir")
    void shouldMapErrors() {
        assertError(new UploadNotAllowedException("file"), HttpStatus.BAD_REQUEST, "UPLOAD_NOT_ALLOWED");
        assertError(
                new ShipmentNotAccessibleException(TestUploads.SHIPMENT_ID),
                HttpStatus.NOT_FOUND,
                "SHIPMENT_NOT_FOUND");
        assertError(new ShipmentLockedException(TestUploads.SHIPMENT_ID), HttpStatus.CONFLICT, "SHIPMENT_LOCKED");
        assertError(
                new ShipmentServiceUnavailableException("down"),
                HttpStatus.SERVICE_UNAVAILABLE,
                "SHIPMENT_SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("Onay hataları: kayıt yok 404, dosya yüklenmemiş 409, uyuşmazlık 422")
    void shouldMapConfirmErrors() {
        assertConfirmError(
                new UploadIntentNotFoundException(TestUploads.EVIDENCE_ID), HttpStatus.NOT_FOUND, "UPLOAD_NOT_FOUND");
        assertConfirmError(
                new UploadNotCompletedException(TestUploads.EVIDENCE_ID), HttpStatus.CONFLICT, "UPLOAD_NOT_COMPLETED");
        assertConfirmError(
                new UploadMismatchException(TestUploads.EVIDENCE_ID),
                HttpStatus.UNPROCESSABLE_CONTENT,
                "UPLOAD_MISMATCH");
    }

    private void assertError(RuntimeException error, HttpStatus status, String code) {
        doThrow(error).when(requestUpload).requestUpload(any());
        MvcTestResult result = post(REQUEST_URI, BODY, warehouse());
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    private void assertConfirmError(RuntimeException error, HttpStatus status, String code) {
        doThrow(error).when(confirmUpload).confirmUpload(any());
        MvcTestResult result = post(CONFIRM_URI, "", warehouse());
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
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
