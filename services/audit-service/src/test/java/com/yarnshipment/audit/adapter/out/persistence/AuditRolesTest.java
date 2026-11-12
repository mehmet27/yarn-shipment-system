package com.yarnshipment.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** audit_writer yalnızca fonksiyon çalıştırır, audit_reader yalnızca okur; uygulama kullanıcısı yetkiyi miras almaz. */
@AuditAdapterTest
class AuditRolesTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.start();
    private static final String PERMISSION_DENIED = "permission denied";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AuditAppendAdapter appender;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    private <T> T asRole(String role, Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            jdbc.sql("SET LOCAL ROLE " + role).update();
            return work.get();
        });
    }

    @Test
    @DisplayName("audit_writer tabloyu okuyamaz ve doğrudan yazamaz")
    void shouldNotLetWriterTouchTables() {
        assertThatThrownBy(() -> asRole(
                        "audit_writer",
                        () -> jdbc.sql("SELECT count(*) FROM audit_log")
                                .query(Long.class)
                                .single()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(PERMISSION_DENIED);
        assertThatThrownBy(() -> asRole(
                        "audit_writer",
                        () -> jdbc.sql("DELETE FROM audit_chain_head").update()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(PERMISSION_DENIED);
    }

    @Test
    @DisplayName("audit_reader okur ama audit_append çalıştıramaz")
    void shouldNotLetReaderAppend() {
        assertThat(asRole(
                        "audit_reader",
                        () -> jdbc.sql("SELECT count(*) FROM audit_log")
                                .query(Long.class)
                                .single()))
                .isNotNegative();
        assertThatThrownBy(() -> asRole(
                        "audit_reader",
                        () -> jdbc.sql("SELECT audit_append('{}'::jsonb)")
                                .query(Long.class)
                                .single()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(PERMISSION_DENIED);
    }

    @Test
    @DisplayName("Uygulama kullanıcısı rollerin yetkisini kendiliğinden miras almaz ama SET ROLE yapabilir")
    void shouldBeMemberWithoutInheriting() {
        Boolean member = jdbc.sql("""
                        SELECT pg_has_role(current_user, 'audit_writer', 'SET')
                           AND pg_has_role(current_user, 'audit_reader', 'SET')""").query(Boolean.class).single();

        assertThat(member).isTrue();
    }

    @Test
    @DisplayName("Adaptör yazdıktan sonra rol geri verilir; aynı transaction'da iş sorguları çalışmaya devam eder")
    void shouldRestoreRoleAfterAppend() {
        String roleAfter = new TransactionTemplate(transactionManager).execute(status -> {
            appender.append(AuditEntries.byUser(
                    UUID.randomUUID(),
                    java.time.Instant.parse("2026-11-12T10:00:00Z"),
                    AuditEntries.LOADED,
                    AuditEntries.SHIPMENT));
            return jdbc.sql("SELECT current_user").query(String.class).single();
        });

        assertThat(roleAfter).isEqualTo("yarnshipment");
    }
}
