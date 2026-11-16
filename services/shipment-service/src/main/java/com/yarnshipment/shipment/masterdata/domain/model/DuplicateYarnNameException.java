package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Aynı adlı başka bir iplik var (409, {@code DUPLICATE_YARN_NAME}); harf büyüklüğü ve kenar boşlukları sayılmaz. */
public final class DuplicateYarnNameException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateYarnNameException(String name) {
        super("Yarn name already used name=" + name);
    }
}
