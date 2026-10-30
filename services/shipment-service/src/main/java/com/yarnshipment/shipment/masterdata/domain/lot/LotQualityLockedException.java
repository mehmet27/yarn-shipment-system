package com.yarnshipment.shipment.masterdata.domain.lot;

import java.io.Serial;
import java.util.UUID;

/** Partinin kalitesi kilitli; parti düzenlenemez ve yeniden kilitlenemez (409, {@code LOT_QUALITY_LOCKED}). */
public final class LotQualityLockedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LotQualityLockedException(UUID lotId) {
        super("Lot quality is locked lotId=" + lotId);
    }
}
