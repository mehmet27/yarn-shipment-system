package com.yarnshipment.shipment.domain.model;

import java.io.Serial;
import java.util.UUID;

/**
 * Tartılan net beklenenin altında; depo eksik kilonun nedenini yazmadan çıkış girilemez (400,
 * {@code SHORTFALL_REASON_REQUIRED}).
 */
public final class ShortfallReasonRequiredException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ShortfallReasonRequiredException(UUID shipmentId) {
        super("Shortfall reason required shipmentId=" + shipmentId);
    }
}
