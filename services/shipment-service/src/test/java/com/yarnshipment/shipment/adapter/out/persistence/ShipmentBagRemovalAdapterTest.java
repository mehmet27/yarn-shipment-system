package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.bag.BagAssignment;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Çuval çıkarma ve yeniden atama, gerçek tetikleyiciler ve kısmi unique indeksle. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    ShipmentBagRemovalAdapter.class,
    BagAssignmentAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(JacksonAutoConfiguration.class)
class ShipmentBagRemovalAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");
    private static final ReservedBag BAG = new ReservedBag(UUID.randomUUID(), "BAG-0001", "P-1", "A");
    private static final BagAssignment ASSIGNMENT = new BagAssignment(SHIPMENT, List.of(BAG), ScanMethod.SCAN);
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, 'SV-3', ?, 'Örnek Tekstil', 'DRAFT', now(), ?)""";
    private static final String ACTIVE_COUNT =
            "SELECT count(*) FROM shipment_bag WHERE shipment_id = ? AND removed_at IS NULL";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentBagRemovalAdapter removal;

    @Autowired
    private BagAssignmentAdapter assignment;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertDraftShipmentWithBag() {
        jdbc.update(INSERT_SHIPMENT, SHIPMENT, SITE, UUID.randomUUID(), USER);
        assignment.assign(ASSIGNMENT, USER, AT);
    }

    @Test
    @DisplayName("Çıkarma satırı silmez, işaretler; ikinci çıkarma 0 satır etkiler")
    void shouldMarkBagRemoved() {
        assertThat(removal.remove(SHIPMENT, "BAG-0001", USER, AT)).isEqualTo(1);

        assertThat(jdbc.queryForObject(ACTIVE_COUNT, Integer.class, SHIPMENT)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM shipment_bag WHERE shipment_id = ?", Integer.class, SHIPMENT))
                .isEqualTo(1);
        assertThat(removal.remove(SHIPMENT, "BAG-0001", USER, AT)).isZero();
    }

    @Test
    @DisplayName("Çıkarılan çuval aynı sevkiyata yeniden atanabilir; aktif çuval ikinci kez eklenmez")
    void shouldAllowReassignmentAfterRemoval() {
        assertThat(assignment.assign(ASSIGNMENT, USER, AT)).isZero();
        removal.remove(SHIPMENT, "BAG-0001", USER, AT);

        assertThat(assignment.assign(ASSIGNMENT, USER, AT)).isEqualTo(1);

        assertThat(jdbc.queryForObject(ACTIVE_COUNT, Integer.class, SHIPMENT)).isEqualTo(1);
    }
}
