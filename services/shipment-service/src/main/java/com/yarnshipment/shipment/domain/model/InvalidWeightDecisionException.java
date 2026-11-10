package com.yarnshipment.shipment.domain.model;

import java.io.Serial;

/** Kantar uyuşmazlığı kararı geçersiz (400, {@code INVALID_WEIGHT_DECISION}). */
public final class InvalidWeightDecisionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidWeightDecisionException(String field, String reason) {
        super("Invalid weight decision field=" + field + " reason=" + reason);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
