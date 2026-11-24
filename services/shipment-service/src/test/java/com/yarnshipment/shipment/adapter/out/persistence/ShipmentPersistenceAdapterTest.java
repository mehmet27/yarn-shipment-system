package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.application.port.in.ListShipmentsQuery.Filter;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.PageRequest;
import com.yarnshipment.shipment.application.port.out.ShipmentQueryPort.ShipmentSlice;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.Weighing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
        insertShipment(new ShipmentRow(FIRST, SITE_A, SUBCONTRACTOR, "AWAITING_DELIVERY", "2026-10-21T06:00:00Z"));
        insertShipment(new ShipmentRow(SECOND, SITE_A, OTHER_SUBCONTRACTOR, "DRAFT", "2026-10-21T07:00:00Z"));
        insertShipment(new ShipmentRow(THIRD, SITE_B, SUBCONTRACTOR, "DRAFT", "2026-10-21T08:00:00Z"));
        insertWeighing(UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1"), "1000.000");
        UUID current = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e2");
        insertWeighing(current, "1010.500");
        jdbc.update(
                "UPDATE weighing SET superseded_by = ? WHERE id = ?",
                current,
                UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1"));
    }

    @Test
    @DisplayName("Sevkiyat geçerli çıkış tartımıyla okunur; düzeltilmiş tartım görünmez")
    void shouldReadShipmentWithCurrentWeighing() {
        Shipment shipment = adapter.findShipment(FIRST).orElseThrow();

        assertThat(shipment.status()).isEqualTo(ShipmentStatus.AWAITING_DELIVERY);
        assertThat(shipment.dispatchWeighing())
                .extracting(Weighing::grossKg)
                .satisfies(gross -> assertThat(gross).isEqualByComparingTo(new BigDecimal("1010.5")));
        assertThat(shipment.waybillNo()).isNull();
    }

    @Test
    @DisplayName("Olmayan sevkiyat boş döner")
    void shouldReturnEmptyForUnknownShipment() {
        assertThat(adapter.findShipment(USER)).isEmpty();
    }

    @Test
    @DisplayName("Liste lokasyon kapsamına göre süzülür, en yeni önce gelir")
    void shouldFilterBySitesNewestFirst() {
        ShipmentSlice slice = adapter.findShipments(AccessScope.restrictedTo(Set.of(SITE_A)), Filter.NONE, FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(SECOND, FIRST);
        assertThat(slice.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("İstenen durumdaki sevkiyatlar lokasyon kapsamında gelir")
    void shouldFilterByStatusWithinSites() {
        ShipmentSlice slice = adapter.findShipments(
                AccessScope.restrictedTo(Set.of(SITE_A, SITE_B)),
                Filter.byStatus(Set.of(ShipmentStatus.DRAFT)),
                FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(THIRD, SECOND);
    }

    @Test
    @DisplayName("Arama metni sevkiyat numarasında büyük-küçük harf ayırmadan aranır")
    void shouldSearchByShipmentNoIgnoringCase() {
        Filter filter = new Filter(Set.of(), "sv-" + SECOND.toString().toUpperCase(Locale.ROOT), null, null);

        ShipmentSlice slice = adapter.findShipments(AccessScope.UNRESTRICTED, filter, FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(SECOND);
        assertThat(slice.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Arama metni fasoncu adında da aranır ve kapsamı genişletmez")
    void shouldSearchBySubcontractorTitleWithinScope() {
        Filter filter = new Filter(Set.of(), "tekstil", null, null);

        ShipmentSlice slice = adapter.findShipments(AccessScope.restrictedTo(Set.of(SITE_B)), filter, FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(THIRD);
    }

    @Test
    @DisplayName("Arama metnindeki yüzde işareti desen sayılmaz")
    void shouldTreatWildcardAsPlainText() {
        Filter filter = new Filter(Set.of(), "%", null, null);

        ShipmentSlice slice = adapter.findShipments(AccessScope.UNRESTRICTED, filter, FIRST_PAGE);

        assertThat(slice.shipments()).isEmpty();
    }

    @Test
    @DisplayName("Sevkiyat tarihi aralığı iki ucu da kapsar")
    void shouldFilterByShipmentDateRange() {
        jdbc.update("UPDATE shipment SET shipment_date = ?::date WHERE id = ?", "2026-10-10", FIRST);
        jdbc.update("UPDATE shipment SET shipment_date = ?::date WHERE id = ?", "2026-10-15", SECOND);
        jdbc.update("UPDATE shipment SET shipment_date = ?::date WHERE id = ?", "2026-10-20", THIRD);
        Filter filter = new Filter(Set.of(), null, LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-15"));

        ShipmentSlice slice = adapter.findShipments(AccessScope.UNRESTRICTED, filter, FIRST_PAGE);

        assertThat(slice.shipments()).extracting(Shipment::id).containsExactly(SECOND, FIRST);
    }

    @Test
    @DisplayName("Lokasyonu olmayan kapsam hiçbir kayıt görmez")
    void shouldReturnNothingForScopeWithoutSites() {
        ShipmentSlice slice = adapter.findShipments(AccessScope.restrictedTo(Set.of()), Filter.NONE, FIRST_PAGE);

        assertThat(slice.shipments()).isEmpty();
        assertThat(slice.totalElements()).isZero();
    }

    @Test
    @DisplayName("Kısıtsız kapsam tüm kayıtları görür")
    void shouldReturnEverythingForUnrestrictedScope() {
        ShipmentSlice slice = adapter.findShipments(AccessScope.UNRESTRICTED, Filter.NONE, FIRST_PAGE);

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
