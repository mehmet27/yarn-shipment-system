package com.yarnshipment.discrepancy.domain.model;

import java.util.UUID;

/** Uyuşmazlık yok. */
public final class DiscrepancyNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DiscrepancyNotFoundException(UUID discrepancyId) {
        super("Discrepancy not found id=" + discrepancyId);
    }
}
