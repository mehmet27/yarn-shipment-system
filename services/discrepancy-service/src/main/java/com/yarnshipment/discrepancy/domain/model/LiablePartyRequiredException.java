package com.yarnshipment.discrepancy.domain.model;

/** Sorumluluk atayan karar sorumlu taraf olmadan verilemez. */
public final class LiablePartyRequiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LiablePartyRequiredException() {
        super("Liable party is required for LIABILITY_ASSIGNED");
    }
}
