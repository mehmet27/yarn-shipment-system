package com.yarnshipment.shipment.domain.correction;

import java.io.Serial;
import java.util.UUID;

/** Düzeltilecek kayıt, istek okunduktan sonra başka bir düzeltmeyle değişti (409, {@code CORRECTION_CONFLICT}). */
public final class CorrectionConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CorrectionConflictException(UUID shipmentId, CorrectionTarget target) {
        super("Correction target changed shipmentId=" + shipmentId + " target=" + target);
    }
}
