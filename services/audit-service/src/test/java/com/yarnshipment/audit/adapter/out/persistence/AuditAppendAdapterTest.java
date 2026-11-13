package com.yarnshipment.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Zincire ekleme, tekrar eden olay ve zincir doğrulama. */
@AuditAdapterTest
class AuditAppendAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.start();
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    @Autowired
    private AuditAppendAdapter appender;

    @Autowired
    private AuditChainAdapter chain;

    @Autowired
    private AuditQueryAdapter queries;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    @Test
    @DisplayName("Aynı olay ikinci kez yazılmaz, ilk kaydın sırası döner")
    void shouldAppendOnce() {
        UUID eventId = UUID.randomUUID();

        long first = appender.append(entry(eventId));
        long again = appender.append(entry(eventId));

        assertThat(again).isEqualTo(first);
    }

    @Test
    @DisplayName("Sağlam zincir doğrulanır ve sonuç okuma tarafında görünür")
    void shouldVerifyIntactChain() {
        appender.append(entry(UUID.randomUUID()));
        long latest = queries.latestRecordId();

        OptionalLong broken = chain.verify(queries.lastVerifiedId() + 1, latest);

        assertThat(broken).isEmpty();
        assertThat(queries.lastVerifiedId()).isEqualTo(latest);
        List<ChainCheck> checks = queries.findLatestChainChecks(1);
        assertThat(checks).hasSize(1);
        assertThat(checks.getFirst().ok()).isTrue();
        assertThat(checks.getFirst().brokenAtId()).isNull();
        assertThat(checks.getFirst().toId()).isEqualTo(latest);
    }

    private static AppendAuditEntryCommand entry(UUID eventId) {
        return AuditEntries.byUser(eventId, AT, AuditEntries.LOADED, AuditEntries.SHIPMENT);
    }
}
