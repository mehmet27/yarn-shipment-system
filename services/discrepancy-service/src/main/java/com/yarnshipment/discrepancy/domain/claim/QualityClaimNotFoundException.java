package com.yarnshipment.discrepancy.domain.claim;

import java.util.UUID;

/** Bu kimlikte kalite itirazı yok. */
public final class QualityClaimNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QualityClaimNotFoundException(UUID claimId) {
        super("Quality claim not found id=" + claimId);
    }
}
