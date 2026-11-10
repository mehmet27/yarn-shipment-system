package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Sevkiyat yaşam döngüsü olayları test verisi. */
final class ShipmentFixtures {

    static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    private ShipmentFixtures() {}

    static ReadModelChange.ShipmentCreated created(UUID id, String no, UUID site, UUID subcontractor) {
        return new ReadModelChange.ShipmentCreated(id, no, site, subcontractor, AT);
    }

    static ReadModelChange.ShipmentDispatched dispatched(UUID id, BigDecimal netKg) {
        return new ReadModelChange.ShipmentDispatched(id, netKg, AT);
    }

    static ReadModelChange.ShipmentCompleted completed(UUID id) {
        return new ReadModelChange.ShipmentCompleted(id, AT);
    }

    static ReadModelChange.ShipmentCancelled cancelled(UUID id) {
        return new ReadModelChange.ShipmentCancelled(id);
    }
}
