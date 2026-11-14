package com.yarnshipment.evidence.application.service;

import com.yarnshipment.evidence.application.port.in.UploadSignedFileUseCase;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Yüklemeyi depoya iletir; imza, süre, boyut ve tür denetimi depo adaptöründedir. */
@Service
public final class UploadSignedFileService implements UploadSignedFileUseCase {

    private final ObjectStoragePort storage;

    public UploadSignedFileService(ObjectStoragePort storage) {
        this.storage = storage;
    }

    @Override
    public void upload(String token, @Nullable String contentType, long contentLength, InputStream body) {
        storage.receiveUpload(token, contentType, contentLength, body);
    }
}
