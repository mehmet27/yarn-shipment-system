package com.yarnshipment.evidence.domain.model;

import java.util.UUID;

/** Kantar fişini yalnızca ilk fişi yükleyen depo çalışanı yeniler (403, {@code SCALE_TICKET_NOT_OWN}). */
public final class ScaleTicketNotOwnedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ScaleTicketNotOwnedException(UUID shipmentId) {
        super("Scale ticket belongs to another user shipmentId=" + shipmentId);
    }
}
