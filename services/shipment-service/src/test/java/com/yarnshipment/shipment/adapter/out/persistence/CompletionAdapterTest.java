package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.shipment.domain.completion.Completion;
import com.yarnshipment.shipment.domain.completion.ShipmentCompleted;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Tamamlama gerçek durum ve kilit tetikleyicileri ile uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    RecordingDomainEventPublisher.class,
    CompletionAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class CompletionAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000401");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-00000000e401");
    private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");
    private static final Completion COMPLETION = new Completion(SHIPMENT, SITE, "IRS-2026-0001", EVIDENCE, NOW);
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-4', ?, 'Örnek Tekstil', 'AWAITING_DELIVERY', now(), ?)""";

    @Autowired
    private RecordingDomainEventPublisher events;

    @Autowired
    private CompletionAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertAwaitingShipment() {
        events.clear();
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, UUID.randomUUID(), USER);
    }

    @Test
    @DisplayName("Tamamlama irsaliyeyi yazar, sevkiyatı kilitler; geçmiş müdür adına, olay yayınlanır")
    void shouldCompleteAndLockShipment() {
        // when
        adapter.complete(COMPLETION, USER);

        // then
        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT e_waybill_no FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("IRS-2026-0001");
        assertThat(jdbc.queryForObject("SELECT waybill_evidence_id FROM shipment WHERE id = ?", UUID.class, SHIPMENT))
                .isEqualTo(EVIDENCE);
        assertThat(jdbc.queryForObject("SELECT locked_at FROM shipment WHERE id = ?", Instant.class, SHIPMENT))
                .isEqualTo(NOW);
        assertThat(jdbc.queryForObject(
                        "SELECT changed_by FROM shipment_status_history WHERE shipment_id = ?", UUID.class, SHIPMENT))
                .isEqualTo(USER);
        assertThat(events.count(SHIPMENT, ShipmentCompleted.TYPE)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Tamamlanmış sevkiyat ikinci kez tamamlanamaz")
    void shouldRejectSecondCompletion() {
        // given
        adapter.complete(COMPLETION, USER);

        // when / then
        assertThatThrownBy(() -> adapter.complete(COMPLETION, USER)).isInstanceOf(InvalidShipmentStatusException.class);
    }
}
