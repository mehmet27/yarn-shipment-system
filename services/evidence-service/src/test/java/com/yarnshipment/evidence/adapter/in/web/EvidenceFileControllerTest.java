package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.application.port.in.DownloadSignedFileUseCase;
import com.yarnshipment.evidence.application.port.in.UploadSignedFileUseCase;
import com.yarnshipment.evidence.domain.model.FileContent;
import com.yarnshipment.evidence.domain.model.SignedFileException;
import com.yarnshipment.evidence.domain.model.SignedFileException.Reason;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** İmzalı dosya adresleri oturumsuz çalışır; hatalar ERR-07 kodlarıyla döner. */
@WebMvcTest(controllers = EvidenceFileController.class)
class EvidenceFileControllerTest {

    private static final String URI = "/api/v1/evidence/files/abc.def";
    private static final byte[] BYTES = "dosya".getBytes(StandardCharsets.UTF_8);

    private final MockMvcTester mvc;

    @MockitoBean
    private UploadSignedFileUseCase uploadFile;

    @MockitoBean
    private DownloadSignedFileUseCase downloadFile;

    @Autowired
    EvidenceFileControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Oturumsuz PUT dosyayı belirteçle birlikte iletir ve 204 döner")
    void shouldAcceptUploadWithoutSession() {
        MvcTestResult result = mvc.put()
                .uri(URI)
                .contentType(MediaType.IMAGE_JPEG)
                .content(BYTES)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(uploadFile).upload(eq("abc.def"), eq("image/jpeg"), eq((long) BYTES.length), any());
    }

    @Test
    @DisplayName("Oturumsuz GET dosyayı türü, boyutu ve güvenlik başlıklarıyla verir")
    void shouldServeDownloadWithoutSession() {
        when(downloadFile.download("abc.def"))
                .thenReturn(new FileContent("image/jpeg", BYTES.length, new ByteArrayInputStream(BYTES)));

        MvcTestResult result = mvc.get().uri(URI).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).hasContentType(MediaType.IMAGE_JPEG);
        assertThat(result).headers().hasValue("X-Content-Type-Options", "nosniff");
        assertThat(result).headers().hasValue("Cache-Control", "no-store, private");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(BYTES);
    }

    @Test
    @DisplayName("İmza, süre, dosya ve uyuşmazlık hataları ERR-07 kodlarına çevrilir")
    void shouldMapSignedFileErrors() {
        assertError(Reason.INVALID_LINK, HttpStatus.FORBIDDEN, "FILE_LINK_INVALID");
        assertError(Reason.EXPIRED_LINK, HttpStatus.GONE, "FILE_LINK_EXPIRED");
        assertError(Reason.SIZE_MISMATCH, HttpStatus.UNPROCESSABLE_CONTENT, "UPLOAD_MISMATCH");
        assertError(Reason.TYPE_MISMATCH, HttpStatus.UNPROCESSABLE_CONTENT, "UPLOAD_MISMATCH");

        when(downloadFile.download(any())).thenThrow(new SignedFileException(Reason.FILE_NOT_FOUND));
        MvcTestResult missing = mvc.get().uri(URI).exchange();
        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(missing).bodyJson().extractingPath("$.code").isEqualTo("FILE_NOT_FOUND");
    }

    private void assertError(Reason reason, HttpStatus status, String code) {
        doThrow(new SignedFileException(reason)).when(uploadFile).upload(any(), any(), anyLong(), any());
        MvcTestResult result = mvc.put()
                .uri(URI)
                .contentType(MediaType.IMAGE_JPEG)
                .content(BYTES)
                .exchange();
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }
}
