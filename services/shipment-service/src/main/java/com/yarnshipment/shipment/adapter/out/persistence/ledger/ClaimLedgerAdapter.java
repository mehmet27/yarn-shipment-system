package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.ClaimLedgerPort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Haklı itirazın bakiye hareketlerinin JDBC uygulaması; defter yazımı ortak yazıcıdan geçer (KR-13). */
@Component
final class ClaimLedgerAdapter implements ClaimLedgerPort {

    private static final String SUBCONTRACTOR = "SELECT subcontractor_id FROM shipment WHERE id = :shipmentId";

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
    public void append(BalanceMovement movement) {
        writer.append(movement);
    }
}
