package com.yarnshipment.audit.adapter.out.persistence;

import com.yarnshipment.audit.application.port.out.AuditChainPort;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link AuditChainPort}'un JDBC uygulaması; {@code audit_verify_chain()} sonucu kendisi kaydeder. */
@Component
final class AuditChainAdapter implements AuditChainPort {

    private final JdbcClient jdbc;

    AuditChainAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OptionalLong verify(long fromId, long toId) {
        Optional<Long> broken = jdbc.sql("SELECT audit_verify_chain(:fromId, :toId)")
                .param("fromId", fromId)
                .param("toId", toId)
                .query(Long.class)
                .optional();
        return broken.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }
}
