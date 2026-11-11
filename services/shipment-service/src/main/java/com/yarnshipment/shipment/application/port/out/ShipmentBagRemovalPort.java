package com.yarnshipment.shipment.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Çuvalı sevkiyatın çuval listesinden çıkarılmış olarak işaretler. */
public interface ShipmentBagRemovalPort {

    /** Çıkarılan satır sayısı; sıra no listede değilse ya da önceden çıkarılmışsa 0. */
    int remove(UUID shipmentId, int seqNo, UUID actorUserId, Instant at);
}
