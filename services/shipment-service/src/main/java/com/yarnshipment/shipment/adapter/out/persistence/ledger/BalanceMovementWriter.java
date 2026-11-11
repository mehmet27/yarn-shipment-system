package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Bakiye defterine satır ekler; defter yalnız eklemeyi kabul eder (KR-13). Aynı {@code source_ref} bir kez yazılır. */
@Component
final class BalanceMovementWriter {

    private static final String INSERT = """
            INSERT INTO balance_movement (id, subcontractor_id, yarn_type, lot_id, shipment_id, kind, net_kg,
                                          occurred_at, created_by, reason, source_ref)
            VALUES (:id, :subcontractorId, :yarnType, :lotId, :shipmentId, :kind, :netKg,
                    :occurredAt, :createdBy, :reason, :sourceRef)
            ON CONFLICT (source_ref) DO NOTHING""";

    private final JdbcClient jdbc;

    BalanceMovementWriter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Satırı ekler; aynı kaynak daha önce yazılmışsa dokunmaz ve 0 döner. */
    int append(BalanceMovement movement) {
        return jdbc.sql(INSERT)
                .param("id", movement.id())
                .param("subcontractorId", movement.subcontractorId())
                .param("yarnType", movement.yarn().yarnType())
                .param("lotId", movement.yarn().lotId())
                .param("shipmentId", movement.shipmentId())
                .param("kind", movement.kind().name())
                .param("netKg", movement.netKg())
                .param("occurredAt", Timestamp.from(movement.occurredAt()))
                .param("createdBy", movement.createdBy())
                .param("reason", movement.reason())
                .param("sourceRef", movement.sourceRef())
                .update();
    }
}
