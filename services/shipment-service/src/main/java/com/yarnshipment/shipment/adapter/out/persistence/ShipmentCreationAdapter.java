package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.application.port.out.ShipmentCreationPort;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.RecipeLine;
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

/**
 * {@link ShipmentCreationPort}'un JDBC uygulaması; reçete satırları ve fire oranı sevkiyatla birlikte yazılır, beklenen
 * net satır toplamıdır. Numara, tarih ve kimliğin rastgele kısmından türetilir.
 */
@Component
final class ShipmentCreationAdapter implements ShipmentCreationPort {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final int SUFFIX_LENGTH = 8;
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id,
                                  driver_name, vehicle_id, plate_no, status, planned_net_kg, fire_rate,
                                  created_at, created_by)
            VALUES (:id, :siteId, :no, :subcontractorId, :title, :driverId, :driverName, :vehicleId, :plateNo,
                    'DRAFT', :plannedNetKg, :fireRate, :at, :actor)""";
    private static final String INSERT_LINE = """
            INSERT INTO shipment_line (id, shipment_id, line_no, yarn_type_id, yarn_type_text, lot_id, net_kg,
                                       fabric_type, lycra, width_type, yarn_length, puss_fine, unit_price,
                                       created_at, created_by)
            VALUES (:id, :shipmentId, :lineNo, :yarnTypeId, :yarnTypeText, :lotId, :netKg,
                    :fabricType, :lycra, :widthType, :yarnLength, :pussFine, :unitPrice, :at, :actor)""";

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
        insertShipment(shipment, new CreatedShipment(id, shipmentNo), at, actorUserId);
        insertLines(shipment, id, at, actorUserId);
        Transition creation = new Transition(id, ShipmentStatus.DRAFT, ShipmentStatus.DRAFT, actorUserId, at);
        transitions.appendInitialHistory(creation);
        transitions.publish(
                creation,
                ShipmentCreated.TYPE,
                new ShipmentCreated(
                        id, shipmentNo, shipment.siteId(), parties.subcontractorId(), shipment.createdAt()));
        return new CreatedShipment(id, shipmentNo);
    }

    private void insertShipment(NewShipment shipment, CreatedShipment created, Timestamp at, UUID actorUserId) {
        UUID id = created.id();
        String shipmentNo = created.shipmentNo();
        ShipmentParties parties = shipment.parties();
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
                .param("plannedNetKg", shipment.plannedNetKg())
                .param("fireRate", shipment.fireRate())
                .param("at", at)
                .param("actor", actorUserId)
                .update();
    }

    private void insertLines(NewShipment shipment, UUID id, Timestamp at, UUID actorUserId) {
        for (RecipeLine line : shipment.lines()) {
            jdbc.sql(INSERT_LINE)
                    .param("id", transitions.nextId())
                    .param("shipmentId", id)
                    .param("lineNo", line.lineNo())
                    .param("yarnTypeId", line.yarnTypeId())
                    .param("yarnTypeText", line.yarnTypeText())
                    .param("lotId", line.lotId())
                    .param("netKg", line.netKg())
                    .param("fabricType", line.fabricType())
                    .param("lycra", line.lycra())
                    .param("widthType", line.widthType() == null ? null : line.widthType().name())
                    .param("yarnLength", line.yarnLength())
                    .param("pussFine", line.pussFine())
                    .param("unitPrice", line.unitPrice())
                    .param("at", at)
                    .param("actor", actorUserId)
                    .update();
        }
    }

    private static String shipmentNo(NewShipment shipment, UUID id) {
        String random = id.toString().replace("-", "");
        String suffix = random.substring(random.length() - SUFFIX_LENGTH).toUpperCase(Locale.ROOT);
        return "SV-" + DAY.format(shipment.createdAt()) + "-" + suffix;
    }
}
