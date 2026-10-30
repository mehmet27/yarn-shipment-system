package com.yarnshipment.shipment.masterdata.domain.model;

import java.io.Serial;

/** Aynı ünvanlı başka bir fasoncu var (409, {@code DUPLICATE_SUBCONTRACTOR}). */
public final class DuplicateSubcontractorException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateSubcontractorException(String title) {
        super("Subcontractor title already used title=" + title);
    }
}
