package com.yarnshipment.discrepancy.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İzlenen sevkiyatın bir adımı.
 *
 * @param shipmentId sevkiyat
 * @param milestone adım
 * @param at adımın zamanı
 * @param netKg çıkış net kilosu; yalnızca {@link Milestone#DISPATCHED} için dolu olabilir
 */
public record TrackShipmentCommand(UUID shipmentId, Milestone milestone, Instant at, @Nullable BigDecimal netKg) {

    /** Kilosuz adım. */
    public TrackShipmentCommand(UUID shipmentId, Milestone milestone, Instant at) {
        this(shipmentId, milestone, at, null);
    }

    public TrackShipmentCommand {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(milestone, "milestone");
        Objects.requireNonNull(at, "at");
    }

    /** Zaman kuralını etkileyen adımlar. */
    public enum Milestone {
        /** Depo çıkış tartısını girdi, sevkiyat yolda. */
        DISPATCHED,
        /** Sevkiyat tamamlandı; kalite itiraz süresi buradan sayılır. */
        COMPLETED,
        /** Sevkiyat iptal edildi. */
        CLOSED
    }
}
