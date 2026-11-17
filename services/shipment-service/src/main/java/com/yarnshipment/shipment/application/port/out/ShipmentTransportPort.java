package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.transport.TransportAssignment;
import java.time.Instant;
import java.util.UUID;

/** Sevkiyatın şoför ve araç bilgisini yazar; verilmeyen bilgi değişmez. */
public interface ShipmentTransportPort {

    /** Yalnızca tamamlanmamış sevkiyat güncellenir; değilse {@code InvalidShipmentStatusException} fırlatır. */
    void updateTransport(UUID shipmentId, TransportAssignment transport, UUID actorUserId, Instant at);
}
