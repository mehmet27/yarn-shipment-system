package com.yarnshipment.evidence.application.port.in;

import com.yarnshipment.evidence.domain.model.EvidenceFile;

/** Yanlış yüklenen kanıtı gerekçeyle geçersiz kılar; kayıt ve dosya silinmez. */
public interface InvalidateEvidenceUseCase {

    EvidenceFile invalidateEvidence(InvalidateEvidenceCommand command);
}
