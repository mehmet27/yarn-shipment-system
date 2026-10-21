package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.PageRequest;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.ShipmentSlice;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Yerel örnek veri gerçek tetikleyici ve kısıtlardan geçer ve servisin okuma yolu onu okuyabilir. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ShipmentPersistenceAdapter.class)
class LocalSampleDataTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.startIsolated();
    private static final UUID SITE_A = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID SITE_B = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    private static final UUID DEMO_SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final PageRequest ALL = new PageRequest(0, 50);
    private static final int SAMPLE_COUNT = 7;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/seed-local");
    }

    @Test
    @DisplayName("Örnek sevkiyatlar her durumdan ve iki lokasyondan okunur")
    void shouldReadSampleShipmentsAcrossStatusesAndSites() {
        ShipmentSlice all = adapter.findShipments(AccessScope.UNRESTRICTED, Set.of(), ALL);

        assertThat(all.totalElements()).isEqualTo(SAMPLE_COUNT);
        assertThat(all.shipments().stream().map(Shipment::status).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        ShipmentStatus.DRAFT,
                        ShipmentStatus.LOADED,
                        ShipmentStatus.IN_TRANSIT,
                        ShipmentStatus.AWAITING_DELIVERY,
                        ShipmentStatus.CHECKING);
        assertThat(all.shipments().stream().map(Shipment::siteId).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(SITE_A, SITE_B);
    }

    @Test
    @DisplayName("Fasoncu kullanıcısı yalnızca kendi örnek sevkiyatlarını görür")
    void shouldScopeSampleShipmentsToSubcontractor() {
        ShipmentSlice own = adapter.findShipments(
                AccessScope.restrictedTo(Set.of(SITE_A), DEMO_SUBCONTRACTOR, null), Set.of(), ALL);

        assertThat(own.shipments())
                .isNotEmpty()
                .allSatisfy(shipment -> assertThat(shipment.subcontractorId()).isEqualTo(DEMO_SUBCONTRACTOR));
    }

    @Test
    @DisplayName("Kontrol aşamasındaki örnekte iki tarafın tartımı ve mührü vardır")
    void shouldCarryBothSidesInCheckingSample() {
        Long weighings = jdbc.queryForObject(
                "SELECT count(*) FROM weighing w JOIN shipment s ON s.id = w.shipment_id WHERE s.status = 'CHECKING'",
                Long.class);

        assertThat(weighings).isEqualTo(2L);
    }
}
