package com.yarnshipment.discrepancy.domain.model;

import java.util.UUID;

/** Uyuşmazlık çözülmüş ya da kapanmış; atama ve karar kabul etmez. */
public final class DiscrepancyNotOpenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DiscrepancyNotOpenException(UUID discrepancyId, DiscrepancyStatus status) {
        super("Discrepancy is not open id=" + discrepancyId + " status=" + status);
    }
}
