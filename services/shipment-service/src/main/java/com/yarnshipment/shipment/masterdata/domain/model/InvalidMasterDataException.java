package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Ana veri bilgisi geçersiz (400, {@code INVALID_MASTER_DATA}). */
public final class InvalidMasterDataException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidMasterDataException(String field, String reason) {
        super("Invalid master data field=" + field + " reason=" + reason);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
