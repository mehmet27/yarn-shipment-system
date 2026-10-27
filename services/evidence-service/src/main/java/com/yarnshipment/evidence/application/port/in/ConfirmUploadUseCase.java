package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.model.EvidenceFile;

/** Yüklemeyi onaylar: dosyayı depodan okuyup hash'ini hesaplar ve kanıt kaydını yazar. Tekrar onay aynı kaydı döner. */
public interface ConfirmUploadUseCase {

    EvidenceFile confirmUpload(ConfirmUploadCommand command);
}
