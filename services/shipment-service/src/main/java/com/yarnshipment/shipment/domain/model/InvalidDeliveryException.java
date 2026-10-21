package com.yarnshipment.shipment.domain.model;

import java.io.Serial;

/** Teslim girişindeki değerler kendi içinde tutarsız (400, {@code INVALID_DELIVERY}). */
public final class InvalidDeliveryException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidDeliveryException(String field, String reason) {
        super("Invalid delivery field=" + field + " reason=" + reason);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
