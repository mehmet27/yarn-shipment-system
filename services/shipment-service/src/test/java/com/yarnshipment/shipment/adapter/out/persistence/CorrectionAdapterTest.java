package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.outbox.OutboxDomainEventPublisher;
import com.yarnshipment.shipment.domain.correction.CorrectedValue.Weight;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.CorrectionConflictException;
import com.yarnshipment.shipment.domain.correction.CorrectionTarget;
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

/** Düzeltme kilitli sevkiyatta gerçek kilit tetikleyicileri ve uygulama rolünün yetkileriyle yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    OutboxDomainEventPublisher.class,
    CorrectionAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class CorrectionAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000501");
    private static final UUID OTHER = UUID.fromString("0192f0a0-0000-7000-8000-000000000502");
    private static final UUID WEIGHING = UUID.fromString("0192f0a0-0000-7000-8000-000000000511");
    private static final UUID WAREHOUSE = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID ADMIN = UUID.fromString("0192f0a0-0000-7000-8000-00000000d002");
    private static final int BAG_COUNT = 40;
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final Weight RECORDED = new Weight(new BigDecimal("2000.5"), new BigDecimal("18"));
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, '0192f0a0-0000-7000-8000-00000000a001', ?, gen_random_uuid(), 'Örnek Tekstil', 'COMPLETED',
                    now(), ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at,
                                  entered_by, created_at, created_by)
            VALUES (?, ?, 'DISPATCH', 2000.5, 18, 40, now(), ?, now(), ?)""";
    private static final String CURRENT_WEIGHING = """
            SELECT gross_kg FROM weighing
            WHERE shipment_id = ? AND side = 'DISPATCH' AND superseded_by IS NULL""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private CorrectionAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    /** İki tamamlanmış ve kilitli sevkiyat; yalnızca ilkinin çıkış tartımı var. */
    @BeforeEach
    void insertLockedShipments() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, "SV-5", WAREHOUSE);
        jdbc.update(INSERT_SHIPMENT, OTHER, "SV-6", WAREHOUSE);
        jdbc.update(INSERT_WEIGHING, WEIGHING, SHIPMENT, WAREHOUSE, WAREHOUSE);
        jdbc.update("UPDATE shipment SET locked_at = now() WHERE id IN (?, ?)", SHIPMENT, OTHER);
    }

    private static Correction correction(CorrectionTarget target, Weight oldValue, Weight newValue) {
        return new Correction(UUID.randomUUID(), SHIPMENT, target, oldValue, newValue, "Fiş yanlış okunmuş", NOW);
    }

    @Test
    @DisplayName("Kilitli sevkiyatın tartımı yeni satırla düzeltilir; eski satır ve eski değer saklanır")
    void shouldCorrectWeighingOfLockedShipment() {
        Weight corrected = new Weight(new BigDecimal("2010.5"), new BigDecimal("18"));
        Correction correction = correction(CorrectionTarget.DISPATCH_WEIGHING, RECORDED, corrected);

        adapter.recordCorrection(correction, ADMIN);

        assertThat(jdbc.queryForObject(CURRENT_WEIGHING, BigDecimal.class, SHIPMENT))
                .isEqualByComparingTo("2010.5");
        assertThat(jdbc.queryForObject(
                        "SELECT bag_count FROM weighing WHERE shipment_id = ? AND superseded_by IS NULL",
                        Integer.class,
                        SHIPMENT))
                .isEqualTo(BAG_COUNT);
        assertThat(jdbc.queryForObject("SELECT gross_kg FROM weighing WHERE id = ?", BigDecimal.class, WEIGHING))
                .isEqualByComparingTo("2000.5");
        var saved = adapter.findCorrection(correction.id()).orElseThrow();
        assertThat(saved.oldValue()).isEqualTo("gross=2000.500 tare=18.000 net=1982.500");
        assertThat(saved.newValue()).isEqualTo("gross=2010.500 tare=18.000 net=1992.500");
        assertThat(saved.status()).isEqualTo("APPROVED");
        assertThat(saved.approvedBy()).isEqualTo(ADMIN);
        assertThat(jdbc.queryForObject(
                        "SELECT entity_id FROM correction_record WHERE id = ?", UUID.class, correction.id()))
                .isEqualTo(WEIGHING);
    }

    @Test
    @DisplayName("Düzeltme kaydı olmadan kilitli sevkiyatın tartımına yazılamaz")
    void shouldKeepLockWithoutCorrection() {
        assertThatThrownBy(() -> jdbc.update(INSERT_WEIGHING, UUID.randomUUID(), SHIPMENT, ADMIN, ADMIN))
                .hasMessageContaining("Shipment is locked");
    }

    @Test
    @DisplayName("Okunan eski değer güncel değilse ya da kayıt yoksa düzeltme çakışma olarak reddedilir")
    void shouldRejectStaleCorrection() {
        Weight stale = new Weight(new BigDecimal("1999"), new BigDecimal("18"));

        assertThatThrownBy(() -> adapter.recordCorrection(
                        correction(CorrectionTarget.DISPATCH_WEIGHING, stale, RECORDED), ADMIN))
                .isInstanceOf(CorrectionConflictException.class);
        Correction withoutWeighing = new Correction(
                UUID.randomUUID(), OTHER, CorrectionTarget.DISPATCH_WEIGHING, RECORDED, stale, "Fiş", NOW);
        assertThatThrownBy(() -> adapter.recordCorrection(withoutWeighing, ADMIN))
                .isInstanceOf(CorrectionConflictException.class);
    }
}
