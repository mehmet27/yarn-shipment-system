package com.yarnshipment.shipment.domain.bag;

import java.io.Serial;

/** Çuval servisi çuvalı sevkiyata ayırmayı reddetti (404 ya da 409). */
public final class BagReservationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Reddin nedeni; web katmanında {@code BAG_*} koduna çevrilir. */
    public enum Reason {
        NOT_FOUND,
        NOT_AVAILABLE,
        ALREADY_RESERVED
    }

    private final Reason reason;

    public BagReservationException(Reason reason) {
        super("Bag reservation rejected reason=" + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
