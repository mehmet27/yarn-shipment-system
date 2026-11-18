package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Haklı itirazın bakiye hareketlerinin JDBC uygulaması; defter yazımı ortak yazıcıdan geçer (KR-13). */
@Component
final class ClaimLedgerAdapter implements ClaimLedgerPort {

    private static final String SUBCONTRACTOR = "SELECT subcontractor_id FROM shipment WHERE id = :shipmentId";

    private static final String INSERT_CLAIM = """
            INSERT INTO fire_claim (discrepancy_id, shipment_id, claimed_kg, decided_by, decided_at, created_at)
            VALUES (:discrepancyId, :shipmentId, :claimedKg, :by, :at, :at)
            ON CONFLICT (discrepancy_id) DO NOTHING""";

    private final JdbcClient jdbc;
    private final BalanceMovementWriter writer;

    ClaimLedgerAdapter(JdbcClient jdbc, BalanceMovementWriter writer) {
        this.jdbc = jdbc;
        this.writer = writer;
    }

    @Override
    public Optional<UUID> findSubcontractor(UUID shipmentId) {
        return jdbc.sql(SUBCONTRACTOR)
                .param("shipmentId", shipmentId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public void recordInvoiceableClaim(InvoiceableClaim claim) {
        jdbc.sql(INSERT_CLAIM)
                .param("discrepancyId", claim.discrepancyId())
                .param("shipmentId", claim.shipmentId())
                .param("claimedKg", claim.claimedKg())
                .param("by", claim.decidedBy())
                .param("at", Timestamp.from(claim.decidedAt()))
                .update();
    }

    @Override
    public void append(BalanceMovement movement) {
        writer.append(movement);
    }
}
