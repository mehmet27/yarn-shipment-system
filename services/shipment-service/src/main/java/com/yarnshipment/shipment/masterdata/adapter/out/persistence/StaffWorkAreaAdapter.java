package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.StaffWorkAreaPort;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link StaffWorkAreaPort}'un JDBC uygulaması. */
@Component
final class StaffWorkAreaAdapter implements StaffWorkAreaPort {

    private static final String UPSERT = """
            INSERT INTO staff_work_area (user_id, work_area) VALUES (:userId, :workArea)
            ON CONFLICT (user_id) DO UPDATE SET work_area = EXCLUDED.work_area""";

    private static final String SELECT_ALL = "SELECT user_id, work_area FROM staff_work_area";

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

    @Override
    public void delete(UUID userId) {
        jdbc.sql("DELETE FROM staff_work_area WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    @Override
    public Map<UUID, StaffWorkArea> findAll() {
        Map<UUID, StaffWorkArea> areas = new HashMap<>();
        jdbc.sql(SELECT_ALL)
                .query((rs, rowNum) -> {
                    areas.put(rs.getObject("user_id", UUID.class), StaffWorkArea.valueOf(rs.getString("work_area")));
                    return rowNum;
                })
                .list();
        return areas;
    }
}
