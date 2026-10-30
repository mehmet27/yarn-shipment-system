package com.yarnshipment.evidence.application.service;

import com.yarnshipment.evidence.application.port.in.DownloadLink;
import com.yarnshipment.evidence.application.port.in.GetEvidenceDownloadUseCase;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedDownload;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import org.springframework.stereotype.Service;

/** Kanıt dosyasının imzalı indirme adresini verir; kanıtı görme kuralları üst veri okumayla aynıdır. */
@Service
public final class GetEvidenceDownloadService implements GetEvidenceDownloadUseCase {

    private final GetEvidenceUseCase getEvidence;
    private final ObjectStoragePort storage;

    public GetEvidenceDownloadService(GetEvidenceUseCase getEvidence, ObjectStoragePort storage) {
        this.getEvidence = getEvidence;
        this.storage = storage;
    }

    @Override
    public DownloadLink getDownloadLink(GetEvidenceQuery query) {
        EvidenceFile file = getEvidence.getEvidence(query);
        PresignedDownload download = storage.presignDownload(file.storageKey());
        return new DownloadLink(download.url(), download.expiresAt());
    }
}
