package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.application.port.out.ShipmentCreationPort;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.ShipmentCreated;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentCreationPort}'un JDBC uygulaması. Numara, tarih ve kimliğin rastgele kısmından türetilir. */
@Component
final class ShipmentCreationAdapter implements ShipmentCreationPort {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final int SUFFIX_LENGTH = 8;
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id,
                                  driver_name, vehicle_id, plate_no, status, created_at, created_by)
            VALUES (:id, :siteId, :no, :subcontractorId, :title, :driverId, :driverName, :vehicleId, :plateNo,
                    'DRAFT', :at, :actor)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    ShipmentCreationAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public CreatedShipment create(NewShipment shipment, UUID actorUserId) {
        UUID id = transitions.nextId();
        String shipmentNo = shipmentNo(shipment, id);
        Timestamp at = Timestamp.from(shipment.createdAt());
        ShipmentParties parties = shipment.parties();
        transitions.actAs(actorUserId);
        jdbc.sql(INSERT_SHIPMENT)
                .param("id", id)
                .param("siteId", shipment.siteId())
                .param("no", shipmentNo)
                .param("subcontractorId", parties.subcontractorId())
                .param("title", parties.subcontractorTitle())
                .param("driverId", parties.driverId())
                .param("driverName", parties.driverName())
                .param("vehicleId", shipment.vehicleId())
                .param("plateNo", parties.plateNo())
                .param("at", at)
                .param("actor", actorUserId)
                .update();
        Transition creation = new Transition(id, ShipmentStatus.DRAFT, ShipmentStatus.DRAFT, actorUserId, at);
        transitions.appendInitialHistory(creation);
        transitions.publish(
                creation,
                ShipmentCreated.TYPE,
                new ShipmentCreated(
                        id, shipmentNo, shipment.siteId(), parties.subcontractorId(), shipment.createdAt()));
        return new CreatedShipment(id, shipmentNo);
    }

    private static String shipmentNo(NewShipment shipment, UUID id) {
        String random = id.toString().replace("-", "");
        String suffix = random.substring(random.length() - SUFFIX_LENGTH).toUpperCase(Locale.ROOT);
        return "SV-" + DAY.format(shipment.createdAt()) + "-" + suffix;
    }
}
