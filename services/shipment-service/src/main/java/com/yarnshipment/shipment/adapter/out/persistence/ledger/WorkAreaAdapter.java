package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.WorkAreaPort;
import com.yarnshipment.shipment.domain.stock.WorkArea;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Çalışma alanı kayıtlarının JDBC uygulaması. */
@Component
final class WorkAreaAdapter implements WorkAreaPort {

    private final JdbcClient jdbc;

    WorkAreaAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<WorkArea> findByUser(UUID userId) {
        return jdbc.sql("SELECT work_area FROM staff_work_area WHERE user_id = ?")
                .param(userId)
                .query(String.class)
                .optional()
                .map(WorkArea::valueOf);
    }
}
