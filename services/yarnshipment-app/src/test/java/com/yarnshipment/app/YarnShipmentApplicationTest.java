package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.events.shipment.ShipmentCreated;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Beş modül tek bağlamda açılır, her şema göç eder ve modüller arası olaylar süreç içi iletilir. */
@SpringBootTest(
        properties = {"yarnshipment.evidence.storage.access-key=test", "yarnshipment.evidence.storage.secret-key=test"})
class YarnShipmentApplicationTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final Instant AT = Instant.parse("2026-11-12T09:00:00Z");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DomainEventPublisher events;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Her modül şeması kendi Flyway geçmişiyle göç eder")
    void shouldMigrateEverySchemaWithItsOwnHistory() {
        List<String> schemas = List.of("shipment", "discrepancy", "evidence", "audit", "reporting");

        for (String schema : schemas) {
            Integer applied = jdbc.queryForObject(
                    "SELECT count(*) FROM " + schema + ".flyway_schema_history WHERE success", Integer.class);
            assertThat(applied).as(schema).isPositive();
        }
    }

    @Test
    @DisplayName("Kafka döneminin tabloları yok: outbox_event, processed_event ve geçici fabric_receipt görünümü")
    void shouldNotHaveRetiredEventTables() {
        List<@Nullable String> found = jdbc.queryForList("""
                SELECT table_schema || '.' || table_name
                  FROM information_schema.tables
                 WHERE table_name IN ('outbox_event', 'processed_event', 'fabric_receipt')""", String.class);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Fire dosyası görünümü kumaş girişini fabric_lot'tan okur")
    void shouldReadFireFileFromFabricLot() {
        String definition =
                jdbc.queryForObject("SELECT pg_get_viewdef('reporting.fire_file_view'::regclass)", String.class);

        assertThat(definition).contains("fabric_lot").doesNotContain("fabric_receipt");
    }

    @Test
    @DisplayName("Sevkiyat olayı commit ile discrepancy izlemesine ve denetim zincirine aynı işlemde yazılır")
    void shouldDeliverShipmentEventToDiscrepancyAndAuditInSameTransaction() {
        UUID shipment = UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> events.publish(created(shipment)));

        assertThat(jdbc.queryForObject(
                        "SELECT shipment_no FROM discrepancy.shipment_watch WHERE shipment_id = ?",
                        String.class,
                        shipment))
                .isEqualTo("SV-APP-1");
        assertThat(jdbc.queryForMap(
                        "SELECT source_service, entity_type, action FROM audit.audit_log WHERE entity_id = ?",
                        shipment))
                .containsEntry("source_service", "shipment-service")
                .containsEntry("entity_type", "Shipment")
                .containsEntry("action", ShipmentCreated.TYPE);
    }

    @Test
    @DisplayName("İş işlemi geri alınırsa izleme kaydı da denetim kaydı da yazılmaz")
    void shouldRollBackConsumersWithBusinessTransaction() {
        UUID shipment = UUID.randomUUID();

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    events.publish(created(shipment));
                    throw new IllegalStateException("iş hatası");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM discrepancy.shipment_watch WHERE shipment_id = ?",
                        Integer.class,
                        shipment))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM audit.audit_log WHERE entity_id = ?", Integer.class, shipment))
                .isZero();
    }

    @Test
    @DisplayName("Ortak tablolar yalnız platform şemasında: modüllerde idempotency_record ve audit_local kopyası yok")
    void shouldKeepSharedTablesInPlatformSchemaOnly() {
        List<@Nullable String> found = jdbc.queryForList("""
                SELECT table_schema || '.' || table_name
                  FROM information_schema.tables
                 WHERE table_name IN ('idempotency_record', 'audit_local') AND table_type = 'BASE TABLE'
                 ORDER BY 1""", String.class);

        assertThat(found).containsExactly("platform.audit_local", "platform.idempotency_record");
    }

    @Test
    @DisplayName("Modül tablosundaki değişiklik platform.audit_local'e şema adı ve aktörle düşer")
    void shouldCaptureTableChangesIntoPlatformAuditLocal() {
        String actor = "app-test-" + UUID.randomUUID();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT set_config('app.actor_id', ?, true)", String.class, actor);
            jdbc.update("""
                    INSERT INTO shipment.app_setting (key, value, updated_at, updated_by)
                    VALUES ('default_fire_rate', '5', now(), ?)
                    ON CONFLICT (key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at""", ACTOR);
        });

        assertThat(jdbc.queryForObject("""
                        SELECT count(*) FROM platform.audit_local
                         WHERE table_schema = 'shipment' AND table_name = 'app_setting' AND actor_id = ?""",
                        Integer.class,
                        actor))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("audit_writer ve audit_reader giriş yapamayan rollerdir; uygulama kullanıcısı ikisine de geçebilir")
    void shouldProvideAuditRoles() {
        assertThat(jdbc.queryForList(
                        "SELECT rolname FROM pg_roles WHERE rolname LIKE 'audit\\_%' AND NOT rolcanlogin ORDER BY 1",
                        String.class))
                .containsExactly("audit_reader", "audit_writer");
        assertThat(jdbc.queryForObject("""
                        SELECT pg_has_role(current_user, 'audit_writer', 'SET')
                           AND pg_has_role(current_user, 'audit_reader', 'SET')""", Boolean.class))
                .isTrue();
    }

    private static DomainEvent created(UUID shipment) {
        return new DomainEvent(
                "Shipment",
                shipment,
                ShipmentCreated.TYPE,
                new ShipmentCreated(shipment, "SV-APP-1", SITE, SUBCONTRACTOR, AT),
                AT,
                ACTOR);
    }
}
