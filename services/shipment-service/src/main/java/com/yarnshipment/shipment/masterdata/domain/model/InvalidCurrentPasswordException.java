package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Şifre değiştirirken girilen mevcut şifre yanlış. */
public final class InvalidCurrentPasswordException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidCurrentPasswordException() {
        super("Current password is wrong");
    }
}
