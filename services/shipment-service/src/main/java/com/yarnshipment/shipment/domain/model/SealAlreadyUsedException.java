package com.yarnshipment.shipment.domain.model;

import java.io.Serial;

/** Mühür numarası başka bir sevkiyatın çıkışında kullanılmış (409, {@code SEAL_ALREADY_USED}). */
public final class SealAlreadyUsedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SealAlreadyUsedException(String sealNo) {
        super("Seal already used sealNo=" + sealNo);
    }
}
