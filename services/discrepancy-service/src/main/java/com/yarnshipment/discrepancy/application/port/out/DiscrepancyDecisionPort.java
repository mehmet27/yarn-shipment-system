package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.time.Instant;
import java.util.UUID;

/** Atama ve karar yazımları; her biri geçmiş kaydını ve outbox olayını aynı transaction'da yazar. */
public interface DiscrepancyDecisionPort {

    /** Atamayı yazar. Uyuşmazlık bu arada çözüldüyse {@code DiscrepancyNotOpenException} fırlatır. */
    void saveAssignment(Discrepancy assigned, UUID actorId, Instant at);

    /** Kararı yazar. Uyuşmazlık bu arada çözüldüyse {@code DiscrepancyNotOpenException} fırlatır. */
    void saveResolution(Discrepancy resolved, UUID actorId, Instant at);
}
