package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.StaffWorkAreaPort;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link StaffWorkAreaPort}'un JDBC uygulaması. */
@Component
final class StaffWorkAreaAdapter implements StaffWorkAreaPort {

    private static final String UPSERT = """
            INSERT INTO staff_work_area (user_id, work_area) VALUES (:userId, :workArea)
            ON CONFLICT (user_id) DO UPDATE SET work_area = EXCLUDED.work_area""";

    private final JdbcClient jdbc;

    StaffWorkAreaAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(UUID userId, StaffWorkArea workArea) {
        jdbc.sql(UPSERT)
                .param("userId", userId)
                .param("workArea", workArea.name())
                .update();
    }
}
