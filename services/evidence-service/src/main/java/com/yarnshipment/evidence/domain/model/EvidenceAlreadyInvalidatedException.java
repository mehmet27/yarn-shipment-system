package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Kanıt zaten geçersiz kılınmış. */
public final class EvidenceAlreadyInvalidatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EvidenceAlreadyInvalidatedException(UUID evidenceId) {
        super("Evidence already invalidated evidenceId=" + evidenceId);
    }
}
