package com.yarnshipment.discrepancy.domain.claim;

/** Kanıt evidence-service'te doğrulanamadı; servis yanıt vermedi. */
public final class EvidenceServiceUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EvidenceServiceUnavailableException(String message) {
        super(message);
    }

    public EvidenceServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
