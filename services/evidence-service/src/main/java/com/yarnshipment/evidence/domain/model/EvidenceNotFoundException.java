package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Kanıt yok ya da isteyene görünmüyor. */
public final class EvidenceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EvidenceNotFoundException(UUID evidenceId) {
        super("Evidence not found evidenceId=" + evidenceId);
    }
}
