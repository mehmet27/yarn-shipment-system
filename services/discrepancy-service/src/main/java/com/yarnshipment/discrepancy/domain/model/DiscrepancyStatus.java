package com.yarnshipment.discrepancy.domain.model;

/** Uyuşmazlığın yaşam döngüsü: açık, incelemede, çözüldü, kapandı. */
public enum DiscrepancyStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    CLOSED;

    /** Atanabilir ve çözülebilir durumlar. */
    public boolean isUnresolved() {
        return this == OPEN || this == IN_REVIEW;
    }
}
