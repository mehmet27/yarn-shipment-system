package com.yarnshipment.discrepancy.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * İzlenen sevkiyatın bir adımı.
 *
 * @param shipmentId sevkiyat
 * @param milestone adım
 * @param at adımın zamanı
 */
public record TrackShipmentCommand(UUID shipmentId, Milestone milestone, Instant at) {

    public TrackShipmentCommand {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(milestone, "milestone");
        Objects.requireNonNull(at, "at");
    }

    /** Zaman kuralını etkileyen adımlar. */
    public enum Milestone {
        /** Şoför teslim aldı, sevkiyat yolda. */
        DISPATCHED,
        /** Şoför fasoncuya vardı. */
        ARRIVED,
        /** Sevkiyat tamamlandı ya da iptal edildi. */
        CLOSED
    }
}
