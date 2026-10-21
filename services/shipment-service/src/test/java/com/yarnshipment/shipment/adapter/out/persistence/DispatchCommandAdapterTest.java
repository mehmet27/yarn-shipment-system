package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.model.DispatchRecording;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.SealAlreadyUsedException;
import com.yarnshipment.shipment.domain.model.Weighing;
import java.math.BigDecimal;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Çıkış kaydı gerçek tetikleyiciler, mühür tekliği kısıtı ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DispatchCommandAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class DispatchCommandAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID OTHER_SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000302");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");
    private static final int BAGS = 40;
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, ?, ?, 'Örnek Tekstil', 'DRAFT', now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private DispatchCommandAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertDraftShipments() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, "SV-D1", UUID.randomUUID(), USER);
        jdbc.update(INSERT_SHIPMENT, OTHER_SHIPMENT, SITE, "SV-D2", UUID.randomUUID(), USER);
    }

    @Test
    @DisplayName("Çıkış kaydı durumu LOADED yapar; tartım, mühür, geçmiş ve olay aynı transaction'da yazılır")
    void shouldRecordDispatch() {
        adapter.recordDispatch(recording(SHIPMENT, "M-0001"), USER);

        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, SHIPMENT))
                .isEqualTo("LOADED");
        assertThat(adapter.hasDispatchWeighing(SHIPMENT)).isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT net_kg::text || ':' || scale_ticket_no FROM weighing WHERE shipment_id = ?",
                        String.class,
                        SHIPMENT))
                .isEqualTo("1982.500:KF-1");
        assertThat(jdbc.queryForObject(
                        "SELECT seal_no || ':' || seal_condition FROM seal WHERE shipment_id = ? AND side = 'DISPATCH'",
                        String.class,
                        SHIPMENT))
                .isEqualTo("M-0001:INTACT");
        assertThat(jdbc.queryForObject(
                        "SELECT to_status FROM shipment_status_history WHERE shipment_id = ?", String.class, SHIPMENT))
                .isEqualTo("LOADED");
        assertThat(jdbc.queryForObject(
                        "SELECT payload ->> 'bagCount' FROM outbox_event WHERE aggregate_id = ?",
                        String.class,
                        SHIPMENT))
                .isEqualTo(String.valueOf(BAGS));
    }

    @Test
    @DisplayName("Başka sevkiyatın çıkışında kullanılmış mühür SealAlreadyUsedException verir")
    void shouldRejectSealUsedByAnotherShipment() {
        adapter.recordDispatch(recording(SHIPMENT, "M-0001"), USER);

        assertThatThrownBy(() -> adapter.recordDispatch(recording(OTHER_SHIPMENT, "M-0001"), USER))
                .isInstanceOf(SealAlreadyUsedException.class);
    }

    @Test
    @DisplayName("Durum arada değiştiyse çıkış yazılmaz ve 409 hatası verilir")
    void shouldRejectWhenStatusChanged() {
        adapter.recordDispatch(recording(SHIPMENT, "M-0001"), USER);

        assertThatThrownBy(() -> adapter.recordDispatch(recording(SHIPMENT, "M-0002"), USER))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Tartımı olmayan sevkiyatta tekrar kontrolü yanlış döner")
    void shouldReportMissingDispatch() {
        assertThat(adapter.hasDispatchWeighing(SHIPMENT)).isFalse();
    }

    private static DispatchRecording recording(UUID shipment, String sealNo) {
        Weighing weighing = new Weighing(new BigDecimal("2000.500"), new BigDecimal("18.000"), BAGS, NOW);
        return new DispatchRecording(shipment, SITE, weighing, "KF-1", sealNo, NOW);
    }
}
