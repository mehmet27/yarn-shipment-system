package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link LoginAccountTargetPort}'un JDBC uygulaması; yalnızca etkin kayıtlar görünür. */
@Component
final class LoginAccountTargetAdapter implements LoginAccountTargetPort {

    private final JdbcClient jdbc;

    LoginAccountTargetAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> findActiveDriverName(UUID id) {
        return jdbc.sql("SELECT full_name FROM md_driver WHERE id = :id AND active")
                .param("id", id)
                .query(String.class)
                .optional();
    }

    @Override
    public Optional<String> findActiveSubcontractorTitle(UUID id) {
        return jdbc.sql("SELECT title FROM md_subcontractor WHERE id = :id AND active")
                .param("id", id)
                .query(String.class)
                .optional();
    }

    @Override
    public Set<UUID> findActiveSiteIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(jdbc.sql("SELECT id FROM md_site WHERE active AND id IN (:ids)")
                .param("ids", ids)
                .query(UUID.class)
                .list());
    }
}
