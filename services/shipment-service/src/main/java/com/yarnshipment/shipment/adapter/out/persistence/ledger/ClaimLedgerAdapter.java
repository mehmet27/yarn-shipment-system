package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Haklı itirazın faturalanacak kilo kaydının JDBC uygulaması. */
@Component
final class ClaimLedgerAdapter implements ClaimLedgerPort {

    private static final String INSERT_CLAIM = """
            INSERT INTO fire_claim (discrepancy_id, shipment_id, claimed_kg, decided_by, decided_at, created_at)
            VALUES (:discrepancyId, :shipmentId, :claimedKg, :by, :at, :at)
            ON CONFLICT (discrepancy_id) DO NOTHING""";

    private final JdbcClient jdbc;

    ClaimLedgerAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
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
}
