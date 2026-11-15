package com.yarnshipment.shipment.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Sevkiyat gününü yazar. */
public interface ShipmentSchedulePort {

    /** Yalnızca taslak sevkiyatın günü değişir; taslak değilse {@code InvalidShipmentStatusException} fırlatır. */
    void updateShipmentDate(UUID shipmentId, LocalDate shipmentDate, UUID actorUserId, Instant at);
}
