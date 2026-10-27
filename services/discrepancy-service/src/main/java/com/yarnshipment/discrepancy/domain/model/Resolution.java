package com.yarnshipment.discrepancy.domain.model;

/** Karar kodları (veritabanındaki {@code resolution} kısıtıyla aynı). */
public enum Resolution {
    TOLERANCE,
    DATA_ERROR,
    CLAIM_JUSTIFIED,
    CLAIM_UNJUSTIFIED,
    LIABILITY_ASSIGNED
}
