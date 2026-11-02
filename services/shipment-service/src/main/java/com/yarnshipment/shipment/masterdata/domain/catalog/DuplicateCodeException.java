package com.yarnshipment.shipment.masterdata.domain.catalog;

import java.io.Serial;

/** Kod ya da parti numarası başka bir kayıtta kullanılıyor (409, {@code DUPLICATE_CODE}). */
public final class DuplicateCodeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String field;

    public DuplicateCodeException(String field, String code) {
        super("Code already used field=" + field + " code=" + code);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
