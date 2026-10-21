package com.yarnshipment.shipment.domain.model;

import java.io.Serial;

/** Çıkış tartımı girişindeki değerler kendi içinde tutarsız (400, {@code INVALID_DISPATCH}). */
public final class InvalidDispatchException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidDispatchException(String field, String reason) {
        super("Invalid dispatch field=" + field + " reason=" + reason);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
