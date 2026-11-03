package com.yarnshipment.bag.domain.model;

import java.io.Serial;
import java.util.UUID;

/** Parti yok ya da kalitesi kilitlenmemiş; çuval üretilemez (409, {@code LOT_NOT_LOCKED}). */
public final class LotNotLockedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LotNotLockedException(UUID lotId) {
        super("Lot quality not locked lotId=" + lotId);
    }

    /** Veritabanı kısıtı ({@code bag_quality_from_locked_lot}) reddettiğinde asıl hata saklanır. */
    public LotNotLockedException(UUID lotId, Throwable cause) {
        super("Lot quality not locked lotId=" + lotId, cause);
    }
}
