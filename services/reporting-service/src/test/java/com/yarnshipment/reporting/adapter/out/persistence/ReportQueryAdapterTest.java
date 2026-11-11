package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.reporting.adapter.out.persistence.SeedRows.Discrepancy;
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
    private static final int THREE = 3;

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
    @DisplayName("Dashboard dönemdeki sevkiyatları, miktarlarını ve vadesi geçen uyuşmazlıkları toplar")
    void shouldBuildDashboard() {
        UUID site = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", "COMPLETED", false));
        seed.shipment(site, new Shipment("SHP-2", "AWAITING_DELIVERY", true));
        seed.shipment(site, new Shipment("SHP-3", "DRAFT", false));
        seed.oldShipment(site);
        seed.discrepancy(site, new Discrepancy("UY-1", "RED", "2026-11-02T00:00:00Z", false));
        seed.discrepancy(site, new Discrepancy("UY-2", "YELLOW", "2026-11-09T00:00:00Z", false));
        seed.discrepancy(site, new Discrepancy("UY-3", "RED", "2026-11-01T00:00:00Z", true));

        var dashboard = reports.findDashboard(PERIOD, scope(site), NOW);

        assertThat(dashboard.shipments().total()).isEqualTo(THREE);
        assertThat(dashboard.shipments().inProgress()).isEqualTo(2);
        assertThat(dashboard.shipments().inReview()).isZero();
        assertThat(dashboard.shipments().completed()).isEqualTo(1);
        assertThat(dashboard.quantity().dispatchNetKg()).isEqualByComparingTo("100");
        assertThat(dashboard.quantity().deliveryNetKg()).isEqualByComparingTo("100");
        assertThat(dashboard.discrepancies().open()).isEqualTo(2);
        assertThat(dashboard.discrepancies().overdue()).isEqualTo(1);
        assertThat(dashboard.overdueDiscrepancies()).extracting("discrepancyNo").containsExactly("UY-1");
    }

    @Test
    @DisplayName("Fasoncu raporu çıkışı yapılmış sevkiyatları uyuşmazlık oranıyla toplar")
    void shouldSumSubcontractorStats() {
        UUID site = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", "COMPLETED", false));
        seed.shipment(site, new Shipment("SHP-2", "AWAITING_DELIVERY", true));
        seed.shipment(site, new Shipment("SHP-3", "AWAITING_DELIVERY", true));

        var subcontractors = reports.findSubcontractorStats(PERIOD, scope(site));

        assertThat(subcontractors).hasSize(1);
        assertThat(subcontractors.get(0).shipmentCount()).isEqualTo(THREE);
        assertThat(subcontractors.get(0).discrepantShipmentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kapsam dışı lokasyonun verisi dönmez; istenen lokasyon kapsamı daraltır")
    void shouldApplySiteScope() {
        UUID site = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.shipment(site, new Shipment("SHP-1", "COMPLETED", false));
        seed.shipment(other, new Shipment("SHP-2", "COMPLETED", false));

        assertThat(reports.findSubcontractorStats(PERIOD, scope(site)).get(0).shipmentCount())
                .isEqualTo(1);
        assertThat(reports.findSubcontractorStats(PERIOD, new ReportScope(Set.of(), false, null)))
                .isEmpty();
        ReportScope narrowed = new ReportScope(Set.of(site, other), false, other);
        assertThat(reports.findSubcontractorStats(PERIOD, narrowed).get(0).shipmentCount())
                .isEqualTo(1);
    }

    private static ReportScope scope(UUID site) {
        return new ReportScope(Set.of(site), false, null);
    }
}
