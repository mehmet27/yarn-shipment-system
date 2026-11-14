package com.yarnshipment.evidence.adapter.in.web;

import com.yarnshipment.evidence.application.port.in.DownloadSignedFileUseCase;
import com.yarnshipment.evidence.application.port.in.UploadSignedFileUseCase;
import com.yarnshipment.evidence.domain.model.FileContent;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Uygulamanın imzaladığı dosya adresleri. Yol oturum ve CSRF istemez ({@code yarnshipment.security.public-paths} ve
 * {@code csrf-ignored-paths}): tarayıcı yüklemeyi çerezsiz yapar ve yetki imzalı belirtecin kendisidir. Belirteç
 * yalnızca tek bir dosya, tür, boyut ve kısa bir süre için geçerlidir.
 */
@RestController
@RequestMapping("/api/v1/evidence/files")
public final class EvidenceFileController {

    private final UploadSignedFileUseCase uploadFile;
    private final DownloadSignedFileUseCase downloadFile;

    public EvidenceFileController(UploadSignedFileUseCase uploadFile, DownloadSignedFileUseCase downloadFile) {
        this.uploadFile = uploadFile;
        this.downloadFile = downloadFile;
    }

    @PutMapping("/{token}")
    public ResponseEntity<Void> upload(@PathVariable String token, HttpServletRequest request) throws IOException {
        uploadFile.upload(token, request.getContentType(), request.getContentLengthLong(), request.getInputStream());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{token}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String token) {
        FileContent file = downloadFile.download(token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.sizeBytes())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(file.content()));
    }
}
