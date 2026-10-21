package com.yarnshipment.shipment.domain.model;

import java.io.Serial;

/** Yeni sevkiyat bilgileri kendi içinde tutarsız (400, {@code INVALID_SHIPMENT}). */
public final class InvalidNewShipmentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidNewShipmentException(String field, String reason) {
        super("Invalid new shipment field=" + field + " reason=" + reason);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
