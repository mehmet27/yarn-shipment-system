package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.DownloadLink;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedDownload;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetEvidenceDownloadServiceTest {

    @Test
    @DisplayName("İndirme adresi, görünen kanıtın nesne anahtarı için depodan alınır")
    void shouldIssueDownloadLink() {
        GetEvidenceUseCase getEvidence = mock(GetEvidenceUseCase.class);
        ObjectStoragePort storage = mock(ObjectStoragePort.class);
        EvidenceFile file = TestUploads.file(EvidenceType.DISPATCH_SCALE);
        GetEvidenceQuery query = new GetEvidenceQuery(TestTokens.actor("warehouse"), file.id());
        when(getEvidence.getEvidence(query)).thenReturn(file);
        when(storage.presignDownload(file.storageKey()))
                .thenReturn(new PresignedDownload("https://s3/get", TestUploads.expiry()));

        DownloadLink link = new GetEvidenceDownloadService(getEvidence, storage).getDownloadLink(query);

        assertThat(link).isEqualTo(new DownloadLink("https://s3/get", TestUploads.expiry()));
        verify(storage).presignDownload(file.storageKey());
    }
}
