package com.yarnshipment.evidence.application.port.in;

import java.io.InputStream;
import org.jspecify.annotations.Nullable;

/** Uygulamanın imzaladığı yükleme adresinin karşılığı; yetki imzalı adresin kendisidir. */
public interface UploadSignedFileUseCase {

    /**
     * İmzalı yükleme adresine gelen dosyayı kaydeder. Boyut ve tür adresteki imzayla bire bir uyuşmalıdır.
     *
     * @param contentLength istemcinin bildirdiği boyut; bildirmediyse negatif
     */
    void upload(String token, @Nullable String contentType, long contentLength, InputStream body);
}
