package com.yarnshipment.shipment.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Çuvalı sevkiyatın çuval listesinden çıkarılmış olarak işaretler. */
public interface ShipmentBagRemovalPort {

    /** Çıkarılan satır sayısı; çuval listede değilse ya da önceden çıkarılmışsa 0. */
    int remove(UUID shipmentId, String uniqueKey, UUID actorUserId, Instant at);
}
