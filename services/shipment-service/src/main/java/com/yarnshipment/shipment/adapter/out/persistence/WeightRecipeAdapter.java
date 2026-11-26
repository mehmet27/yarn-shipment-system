package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort.Audit;
import com.yarnshipment.shipment.application.port.out.WeightRecipePort;
import com.yarnshipment.shipment.domain.model.OrderSpec;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.sql.Timestamp;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link WeightRecipePort}'un JDBC uygulaması. Reçete satırları silinemez: mevcut satır yerinde düzeltilir, fazla
 * satır eklenir. Değişiklikler {@code audit_local} izine düşer.
 */
@Component
final class WeightRecipeAdapter implements WeightRecipePort {

    private static final String UPDATE_LINE = """
            UPDATE shipment_line
            SET yarn_type_id = :yarnTypeId, yarn_type_text = :yarnTypeText, lot_id = :lotId, net_kg = :netKg,
                fabric_type = :fabricType, lycra = :lycra, width_type = :widthType, yarn_length = :yarnLength,
                puss_fine = :pussFine, unit_price = :unitPrice,
                updated_at = :at, updated_by = :actor, version = version + 1
            WHERE shipment_id = :shipmentId AND line_no = :lineNo""";
    private static final String INSERT_LINE = """
            INSERT INTO shipment_line (id, shipment_id, line_no, yarn_type_id, yarn_type_text, lot_id, net_kg,
                                       fabric_type, lycra, width_type, yarn_length, puss_fine, unit_price,
                                       created_at, created_by)
            VALUES (:id, :shipmentId, :lineNo, :yarnTypeId, :yarnTypeText, :lotId, :netKg,
                    :fabricType, :lycra, :widthType, :yarnLength, :pussFine, :unitPrice, :at, :actor)""";
    private static final String UPDATE_PLANNED = """
            UPDATE shipment SET planned_net_kg = :netKg, updated_at = :at, updated_by = :actor, version = version + 1
            WHERE id = :shipmentId""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    WeightRecipeAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public void replaceAndDispatch(RecipeReplacement replacement, String reason, Audit audit) {
        UUID shipmentId = replacement.shipmentId();
        UUID actorUserId = audit.actorUserId();
        Timestamp when = Timestamp.from(audit.at());
        transitions.actAs(actorUserId);
        for (RecipeLine line : replacement.lines()) {
            RecipeLotGuard.require(jdbc, line);
            writeLine(line, new Write(shipmentId, when, actorUserId));
        }
        jdbc.sql(UPDATE_PLANNED)
                .param("netKg", replacement.totalKg())
                .param("at", when)
                .param("actor", actorUserId)
                .param("shipmentId", shipmentId)
                .update();
        Transition transition = new Transition(
                shipmentId,
                ShipmentStatus.WEIGHT_MISMATCH,
                ShipmentStatus.AWAITING_DELIVERY,
                actorUserId,
                when,
                reason);
        transitions.changeStatus(transition);
        ShipmentDispatched event = replacement.event();
        transitions.publish(transition, ShipmentDispatched.TYPE, event);
    }

    private void writeLine(RecipeLine line, Write write) {
        int updated = bind(jdbc.sql(UPDATE_LINE), line, write).update();
        if (updated == 0) {
            bind(jdbc.sql(INSERT_LINE).param("id", transitions.nextId()), line, write)
                    .update();
        }
    }

    /** Satırın yazıldığı sevkiyat, an ve kullanıcı. */
    private record Write(UUID shipmentId, Timestamp when, UUID actorUserId) {}

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, RecipeLine line, Write write) {
        OrderSpec spec = line.orderSpec();
        return statement
                .param("shipmentId", write.shipmentId())
                .param("lineNo", line.lineNo())
                .param("yarnTypeId", line.yarnTypeId())
                .param("yarnTypeText", line.yarnTypeText())
                .param("lotId", line.lotId())
                .param("netKg", line.netKg())
                .param("fabricType", spec.fabricType())
                .param("lycra", spec.lycra())
                .param("widthType", widthTypeName(spec))
                .param("yarnLength", spec.yarnLength())
                .param("pussFine", spec.pussFine())
                .param("unitPrice", spec.unitPrice())
                .param("at", write.when())
                .param("actor", write.actorUserId());
    }

    private static @Nullable String widthTypeName(OrderSpec spec) {
        return spec.widthType() == null ? null : spec.widthType().name();
    }
}
