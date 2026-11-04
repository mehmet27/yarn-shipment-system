package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.reporting.adapter.out.persistence.SeedRows.Discrepancy;
import com.yarnshipment.reporting.adapter.out.persistence.SeedRows.Pair;
import com.yarnshipment.reporting.adapter.out.persistence.SeedRows.Shipment;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Dashboard ve rapor sorguları; gerçek şemada, uygulama rolüyle. Her test kendi lokasyonunu kullanır. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    ReportQueryAdapter.class,
    DashboardReader.class,
    DiscrepancyReader.class,
    PartyReader.class,
    ShipmentListReader.class
})
class ReportQueryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final ReportPeriod PERIOD =
            new ReportPeriod(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"));
    private static final UUID DRIVER_A = UUID.randomUUID();
    private static final UUID DRIVER_B = UUID.randomUUID();
    private static final int THREE = 3;
    private static final int FOUR = 4;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ReportQueryAdapter reports;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Dashboard dönemdeki sevkiyatları, bantları, farkları ve vadesi geçen uyuşmazlıkları toplar")
    void shouldBuildDashboard() {
        UUID site = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", DRIVER_A, "COMPLETED", "GREEN", "0.000", false));
        seed.shipment(site, new Shipment("SHP-2", DRIVER_A, "IN_REVIEW", "RED", "-40.000", true));
        seed.shipment(site, new Shipment("SHP-3", DRIVER_B, "IN_TRANSIT", null, null, false));
        seed.oldShipment(site);
        seed.discrepancy(site, new Discrepancy("UY-1", "RED", "2026-11-02T00:00:00Z", false));
        seed.discrepancy(site, new Discrepancy("UY-2", "YELLOW", "2026-11-09T00:00:00Z", false));
        seed.discrepancy(site, new Discrepancy("UY-3", "RED", "2026-11-01T00:00:00Z", true));

        var dashboard = reports.findDashboard(PERIOD, scope(site), NOW);

        assertThat(dashboard.shipments().total()).isEqualTo(THREE);
        assertThat(dashboard.shipments().inProgress()).isEqualTo(1);
        assertThat(dashboard.shipments().inReview()).isEqualTo(1);
        assertThat(dashboard.shipments().completed()).isEqualTo(1);
        assertThat(dashboard.quantity().comparedShipments()).isEqualTo(2);
        assertThat(dashboard.quantity().absoluteDiffKg()).isEqualByComparingTo("40.000");
        assertThat(dashboard.bands().green()).isEqualTo(1);
        assertThat(dashboard.bands().red()).isEqualTo(1);
        assertThat(dashboard.discrepancies().open()).isEqualTo(2);
        assertThat(dashboard.discrepancies().overdue()).isEqualTo(1);
        assertThat(dashboard.overdueDiscrepancies()).extracting("discrepancyNo").containsExactly("UY-1");
        assertThat(dashboard.largestDifferences()).extracting("shipmentNo").containsExactly("SHP-2", "SHP-1");
    }

    @Test
    @DisplayName("Şoför raporu en yüksek uyuşmazlık oranını öne alır; fasoncu raporu aynı sevkiyatları toplar")
    void shouldRankDriversByDiscrepancyRate() {
        UUID site = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", DRIVER_A, "COMPLETED", "GREEN", "0.000", false));
        seed.shipment(site, new Shipment("SHP-2", DRIVER_A, "IN_REVIEW", "RED", "-40.000", true));
        seed.shipment(site, new Shipment("SHP-3", DRIVER_B, "IN_REVIEW", "YELLOW", "10.000", true));

        var drivers = reports.findDriverStats(PERIOD, scope(site));
        var subcontractors = reports.findSubcontractorStats(PERIOD, scope(site));

        assertThat(drivers).extracting(party -> party.id()).containsExactly(DRIVER_B, DRIVER_A);
        assertThat(drivers.get(1).discrepancyRate()).isEqualByComparingTo("50.00");
        assertThat(drivers.get(1).averageAbsoluteDiffKg()).isEqualByComparingTo("20.000");
        assertThat(subcontractors).hasSize(1);
        assertThat(subcontractors.get(0).shipmentCount()).isEqualTo(THREE);
    }

    @Test
    @DisplayName("Kapsam dışı lokasyonun verisi dönmez; istenen lokasyon kapsamı daraltır")
    void shouldApplySiteScope() {
        UUID site = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", DRIVER_A, "COMPLETED", "GREEN", "0.000", false));
        seed.shipment(other, new Shipment("SHP-2", DRIVER_A, "COMPLETED", "GREEN", "0.000", false));

        assertThat(reports.findDriverStats(PERIOD, scope(site)).get(0).shipmentCount())
                .isEqualTo(1);
        assertThat(reports.findDriverStats(PERIOD, new ReportScope(Set.of(), false, null)))
                .isEmpty();
        ReportScope narrowed = new ReportScope(Set.of(site, other), false, other);
        assertThat(reports.findDriverStats(PERIOD, narrowed).get(0).shipmentCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Eşleşme raporu günlük sayaçları dönemde toplar")
    void shouldSumPairStats() {
        UUID site = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.pair(site, DRIVER_A, new Pair("2026-11-02", 2, 1, 2, "30.000"));
        seed.pair(site, DRIVER_A, new Pair("2026-11-03", 2, 0, 0, "10.000"));
        seed.pair(site, DRIVER_A, new Pair("2026-10-01", 1, 1, 1, "5.000"));

        var pairs = reports.findPairStats(PERIOD, scope(site));

        assertThat(pairs).hasSize(1);
        assertThat(pairs.get(0).shipmentCount()).isEqualTo(FOUR);
        assertThat(pairs.get(0).discrepancyRate()).isEqualByComparingTo("25.00");
        assertThat(pairs.get(0).averageAbsoluteDiffKg()).isEqualByComparingTo("10.000");
    }

    @Test
    @DisplayName("Sevkiyat dökümü dönemdeki satırları en yeni önce, kapsamla sınırlı ve limitli döner")
    void shouldListShipments() {
        UUID site = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", DRIVER_A, "COMPLETED", "GREEN", "0.000", false));
        seed.shipment(site, new Shipment("SHP-2", DRIVER_B, "IN_REVIEW", "RED", "-40.000", true));
        seed.shipment(site, new Shipment("SHP-3", DRIVER_B, "IN_TRANSIT", null, null, false));
        seed.shipment(other, new Shipment("SHP-X", DRIVER_A, "COMPLETED", "GREEN", "0.000", false));
        seed.oldShipment(site);

        var rows = reports.findShipments(PERIOD, scope(site), 10);

        assertThat(rows).extracting("shipmentNo").containsExactly("SHP-1", "SHP-2", "SHP-3");
        var red = rows.get(1);
        assertThat(red.hadDiscrepancy()).isTrue();
        assertThat(red.band()).isEqualTo("RED");
        assertThat(red.diffKg()).isEqualByComparingTo("-40.000");
        assertThat(rows.get(2).band()).isNull();
        assertThat(rows.get(2).completedAt()).isNull();
        assertThat(reports.findShipments(PERIOD, scope(site), 2)).hasSize(2);
        assertThat(reports.findShipments(PERIOD, new ReportScope(Set.of(), false, null), 10))
                .isEmpty();
    }

    @Test
    @DisplayName("Fasoncu bakiyesi iptalleri saymaz; teslimi olmayan çıkışı yolda kalan olarak ayırır")
    void shouldSummarizeSubcontractorBalance() {
        UUID site = UUID.randomUUID();
        UUID subcontractor = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.balanceShipment(site, subcontractor, "COMPLETED", "1000.000", "990.000");
        seed.balanceShipment(site, subcontractor, "IN_TRANSIT", "500.000", null);
        seed.balanceShipment(site, subcontractor, "CANCELLED", "300.000", null);

        var balances = reports.findSubcontractorBalances(PERIOD, scope(site));

        assertThat(balances).hasSize(1);
        var balance = balances.get(0);
        assertThat(balance.subcontractorId()).isEqualTo(subcontractor);
        assertThat(balance.shipmentCount()).isEqualTo(2);
        assertThat(balance.dispatchedKg()).isEqualByComparingTo("1500.000");
        assertThat(balance.deliveredKg()).isEqualByComparingTo("990.000");
        assertThat(balance.inTransitKg()).isEqualByComparingTo("500.000");
        assertThat(balance.differenceKg()).isEqualByComparingTo("10.000");
    }

    private static ReportScope scope(UUID site) {
        return new ReportScope(Set.of(site), false, null);
    }
}
