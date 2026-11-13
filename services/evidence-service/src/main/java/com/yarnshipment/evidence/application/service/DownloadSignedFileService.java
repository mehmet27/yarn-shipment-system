package com.yarnshipment.evidence.application.service;

import com.yarnshipment.evidence.application.port.in.DownloadSignedFileUseCase;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.domain.model.FileContent;
import org.springframework.stereotype.Service;

/** İndirmeyi depodan açar; imza ve süre denetimi depo adaptöründedir. */
@Service
public final class DownloadSignedFileService implements DownloadSignedFileUseCase {

    private final ObjectStoragePort storage;

    public DownloadSignedFileService(ObjectStoragePort storage) {
        this.storage = storage;
    }

    @Override
    public FileContent download(String token) {
        return storage.openDownload(token);
    }
}
