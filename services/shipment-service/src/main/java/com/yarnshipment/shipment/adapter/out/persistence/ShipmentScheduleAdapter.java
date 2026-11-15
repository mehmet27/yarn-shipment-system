package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentSchedulePort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentSchedulePort}'un JDBC uygulaması; yalnızca taslak sevkiyatın günü değişir. */
@Component
final class ShipmentScheduleAdapter implements ShipmentSchedulePort {

    private static final String UPDATE = """
            UPDATE shipment
            SET shipment_date = :day, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :id AND status = 'DRAFT'""";

    private final JdbcClient jdbc;

    ShipmentScheduleAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void updateShipmentDate(UUID shipmentId, LocalDate shipmentDate, UUID actorUserId, Instant at) {
        int updated = jdbc.sql(UPDATE)
                .param("day", shipmentDate)
                .param("at", Timestamp.from(at))
                .param("actor", actorUserId)
                .param("id", shipmentId)
                .update();
        if (updated == 0) {
            throw new InvalidShipmentStatusException(shipmentId, ShipmentStatus.DRAFT, ShipmentStatus.DRAFT);
        }
    }
}
