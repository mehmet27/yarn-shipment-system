package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentTransportPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.transport.TransportAssignment;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentTransportPort}'un JDBC uygulaması; verilmeyen bilgi korunur, tamamlanmış sevkiyat değişmez. */
@Component
final class ShipmentTransportAdapter implements ShipmentTransportPort {

    private static final String UPDATE = """
            UPDATE shipment
            SET driver_id = coalesce(:driverId, driver_id), driver_name = coalesce(:driverName, driver_name),
                vehicle_id = coalesce(:vehicleId, vehicle_id), plate_no = coalesce(:plateNo, plate_no),
                updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :id AND status = 'DRAFT'""";

    private final JdbcClient jdbc;

    ShipmentTransportAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void updateTransport(UUID shipmentId, TransportAssignment transport, UUID actorUserId, Instant at) {
        int updated = jdbc.sql(UPDATE)
                .param("driverId", transport.driverId())
                .param("driverName", transport.driverName())
                .param("vehicleId", transport.vehicleId())
                .param("plateNo", transport.plateNo())
                .param("at", Timestamp.from(at))
                .param("actor", actorUserId)
                .param("id", shipmentId)
                .update();
        if (updated == 0) {
            throw new InvalidShipmentStatusException(shipmentId, ShipmentStatus.DRAFT, ShipmentStatus.DRAFT);
        }
    }
}
