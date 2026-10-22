package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Plaka başka bir araçta kullanılıyor (409, {@code DUPLICATE_PLATE}). */
public final class DuplicatePlateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicatePlateException(String plateNo) {
        super("Plate already used plate=" + plateNo);
    }
}
