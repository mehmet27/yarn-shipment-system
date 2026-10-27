package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Dosya nesne deposunda yok; yükleme bitmeden onaylandı. */
public final class UploadNotCompletedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UploadNotCompletedException(UUID evidenceId) {
        super("Uploaded object is missing evidenceId=" + evidenceId);
    }
}
