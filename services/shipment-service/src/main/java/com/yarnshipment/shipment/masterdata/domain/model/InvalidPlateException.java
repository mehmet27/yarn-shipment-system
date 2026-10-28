package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Plaka Türkiye plaka biçiminde değil (400, {@code INVALID_PLATE}). */
public final class InvalidPlateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidPlateException(String plateNo) {
        super("Invalid plate plate=" + plateNo);
    }
}
