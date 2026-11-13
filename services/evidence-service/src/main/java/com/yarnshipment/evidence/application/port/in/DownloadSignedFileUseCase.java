package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.model.FileContent;

/** Uygulamanın imzaladığı indirme adresinin karşılığı; yetki imzalı adresin kendisidir. */
public interface DownloadSignedFileUseCase {

    /** İmzalı indirme adresindeki dosyayı açar; akışı çağıran kapatır. */
    FileContent download(String token);
}
