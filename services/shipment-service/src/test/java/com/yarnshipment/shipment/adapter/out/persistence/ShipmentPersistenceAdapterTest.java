package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.PageRequest;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.ShipmentSlice;
import com.yarnshipment.shipment.domain.model.SealCondition;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.Weighing;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

/** Migration'lar gerçek PostgreSQL'e uygulanır; uygulama yalnızca DML rolüyle bağlanır. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ShipmentPersistenceAdapter.class)
class ShipmentPersistenceAdapterTest {

    private static final UUID SITE_A = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID SITE_B = UUID.fromString("0192f0a0-0000-7000-8000-00000000b001");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000c001");
    private static final UUID OTHER_SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000c002");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID FIRST = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID SECOND = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    private static final UUID THIRD = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final PageRequest FIRST_PAGE = new PageRequest(0, 10);
    private static final int ALL_SHIPMENTS = 3;
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                  created_at, created_by)
            VALUES (?, ?, ?, ?, 'Örnek Tekstil', ?, ?::timestamptz, ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                                  created_at, created_by)
            VALUES (?, ?, 'DISPATCH', ?::numeric, 18, 40, now(), ?, now(), ?)""";
    private static final String INSERT_SEAL = """
            INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by, recorded_at,
                              created_at, created_by)
            VALUES (?, ?, 'DISPATCH', 'M-0001', 'INTACT', ?, now(), now(), ?)""";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @BeforeEach
    void insertShipments() {
        insertShipment(new ShipmentRow(FIRST, SITE_A, SUBCONTRACTOR, "IN_TRANSIT", "2026-10-21T06:00:00Z"));
        insertShipment(new ShipmentRow(SECOND, SITE_A, OTHER_SUBCONTRACTOR, "DRAFT", "2026-10-21T07:00:00Z"));
        insertShipment(new ShipmentRow(THIRD, SITE_B, SUBCONTRACTOR, "DRAFT", "2026-10-21T08:00:00Z"));
        insertWeighing(UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1"), "1000.000");
        UUID current = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e2");
        insertWeighing(current, "1010.500");
        jdbc.update(
                "UPDATE weighing SET superseded_by = ? WHERE id = ?",
                current,
                UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1"));
        jdbc.update(INSERT_SEAL, UUID.fromString("0192f0a0-0000-7000-8000-0000000000f1"), FIRST, USER, USER);
    }

    @Test
    @DisplayName("Sevkiyat geçerli tartım ve mühürüyle okunur; düzeltilmiş tartım görünmez")
    void shouldReadShipmentWithCurrentWeighingAndSeal() {
        Shipment shipment = adapter.findShipment(FIRST).orElseThrow();

        assertThat(shipment.status()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(shipment.dispatch().weighing())
                .extracting(Weighing::grossKg)
                .satisfies(gross -> assertThat(gross).isEqualByComparingTo(new BigDecimal("1010.5")));
        assertThat(shipment.dispatch().sealCondition()).isEqualTo(SealCondition.INTACT);
        assertThat(shipment.delivery().weighing()).isNull();
    }

    @Test
    @DisplayName("Olmayan sevkiyat boş döner")
    void shouldReturnEmptyForUnknownShipment() {
        assertThat(adapter.findShipment(USER)).isEmpty();
    }

    @Test
    @DisplayName("Liste lokasyon kapsamına göre süzülür, en yeni önce gelir")
    void shouldFilterBySitesNewestFirst() {
        ShipmentSlice slice =
                adapter.findShipments(AccessScope.restrictedTo(Set.of(SITE_A), null, null), Set.of(), FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(SECOND, FIRST);
        assertThat(slice.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Fasoncu yalnızca kendi sevkiyatlarını, istenen durumda görür")
    void shouldFilterBySubcontractorAndStatus() {
        ShipmentSlice slice = adapter.findShipments(
                AccessScope.restrictedTo(Set.of(SITE_A, SITE_B), SUBCONTRACTOR, null),
                Set.of(ShipmentStatus.DRAFT),
                FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(THIRD);
    }

    @Test
    @DisplayName("Lokasyonu olmayan kapsam hiçbir kayıt görmez")
    void shouldReturnNothingForScopeWithoutSites() {
        ShipmentSlice slice =
                adapter.findShipments(AccessScope.restrictedTo(Set.of(), null, null), Set.of(), FIRST_PAGE);

        assertThat(slice.shipments()).isEmpty();
        assertThat(slice.totalElements()).isZero();
    }

    @Test
    @DisplayName("Kısıtsız kapsam tüm kayıtları görür")
    void shouldReturnEverythingForUnrestrictedScope() {
        ShipmentSlice slice = adapter.findShipments(AccessScope.UNRESTRICTED, Set.of(), FIRST_PAGE);

        assertThat(slice.totalElements()).isEqualTo(ALL_SHIPMENTS);
    }

    private void insertShipment(ShipmentRow row) {
        jdbc.update(
                INSERT_SHIPMENT,
                row.id(),
                row.site(),
                "SV-" + row.id(),
                row.subcontractor(),
                row.status(),
                row.createdAt(),
                USER);
    }

    private void insertWeighing(UUID id, String grossKg) {
        jdbc.update(INSERT_WEIGHING, id, FIRST, grossKg, USER, USER);
    }

    private record ShipmentRow(UUID id, UUID site, UUID subcontractor, String status, String createdAt) {}
}
