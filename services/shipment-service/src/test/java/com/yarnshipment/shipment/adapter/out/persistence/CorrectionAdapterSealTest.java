package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.correction.CorrectedValue.SealNo;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.CorrectionTarget;
import com.yarnshipment.shipment.domain.model.SealAlreadyUsedException;
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

/** Mühür düzeltmesi kilitli sevkiyatta, tek kullanımlık mühür kısıtıyla birlikte yazılır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CorrectionAdapter.class, ShipmentTransitionWriter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class CorrectionAdapterSealTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000501");
    private static final UUID OTHER = UUID.fromString("0192f0a0-0000-7000-8000-000000000502");
    private static final UUID WAREHOUSE = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID ADMIN = UUID.fromString("0192f0a0-0000-7000-8000-00000000d002");
    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, '0192f0a0-0000-7000-8000-00000000a001', ?, gen_random_uuid(), 'Örnek Tekstil', 'COMPLETED',
                    now(), ?)""";
    private static final String INSERT_SEAL = """
            INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by, recorded_at, created_at,
                              created_by)
            VALUES (gen_random_uuid(), ?, 'DISPATCH', ?, 'INTACT', ?, now(), now(), ?)""";

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

    /** İki tamamlanmış ve kilitli sevkiyat; mühürleri M-0001 ve M-0009. */
    @BeforeEach
    void insertLockedShipments() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, "SV-5", WAREHOUSE);
        jdbc.update(INSERT_SHIPMENT, OTHER, "SV-6", WAREHOUSE);
        jdbc.update(INSERT_SEAL, SHIPMENT, "M-0001", WAREHOUSE, WAREHOUSE);
        jdbc.update(INSERT_SEAL, OTHER, "M-0009", WAREHOUSE, WAREHOUSE);
        jdbc.update("UPDATE shipment SET locked_at = now() WHERE id IN (?, ?)", SHIPMENT, OTHER);
    }

    private static Correction correction(CorrectionTarget target, SealNo oldValue, SealNo newValue) {
        return new Correction(UUID.randomUUID(), SHIPMENT, target, oldValue, newValue, "Fiş yanlış okunmuş", NOW);
    }

    @Test
    @DisplayName("Mühür düzeltilir ve düzeltmeler sırayla okunur; başka sevkiyatın mührü kullanılamaz")
    void shouldCorrectSealAndRejectUsedNumber() {
        adapter.recordCorrection(
                correction(CorrectionTarget.DISPATCH_SEAL_NO, new SealNo("M-0001"), new SealNo("M-0002")), ADMIN);

        var corrections = adapter.findCorrections(SHIPMENT);

        assertThat(corrections).singleElement().satisfies(saved -> {
            assertThat(saved.target()).isEqualTo(CorrectionTarget.DISPATCH_SEAL_NO);
            assertThat(saved.newValue()).isEqualTo("M-0002");
        });
        assertThatThrownBy(() -> adapter.recordCorrection(
                        correction(CorrectionTarget.DISPATCH_SEAL_NO, new SealNo("M-0002"), new SealNo("M-0009")),
                        ADMIN))
                .isInstanceOf(SealAlreadyUsedException.class);
    }
}
