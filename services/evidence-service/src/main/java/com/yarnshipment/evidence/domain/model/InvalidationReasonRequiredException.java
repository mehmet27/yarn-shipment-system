package com.yarnshipment.evidence.domain.model;

/** Geçersiz kılma gerekçesi boş olamaz. */
public final class InvalidationReasonRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidationReasonRequiredException() {
        super("Invalidation reason required");
    }
}
