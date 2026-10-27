package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Yüklenen dosyanın boyutu ya da türü bildirilenle aynı değil. */
public final class UploadMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UploadMismatchException(UUID evidenceId) {
        super("Uploaded file differs from the declaration evidenceId=" + evidenceId);
    }
}
