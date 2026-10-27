package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Yükleme isteği yok. */
public final class UploadIntentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UploadIntentNotFoundException(UUID evidenceId) {
        super("Upload intent not found evidenceId=" + evidenceId);
    }
}
