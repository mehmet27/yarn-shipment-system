package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.reporting.adapter.out.persistence.SeedRows.Balance;
import com.yarnshipment.reporting.adapter.out.persistence.SeedRows.Shipment;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
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

/** Sevkiyat dökümü ve fasoncu bakiyesi sorguları; gerçek şemada. Her test kendi lokasyonunu kullanır. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ShipmentListReader.class)
class ShipmentListReaderTest {

    private static final ReportPeriod PERIOD =
            new ReportPeriod(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"));
    private static final UUID DRIVER_A = UUID.randomUUID();
    private static final UUID DRIVER_B = UUID.randomUUID();
    private static final int LIMIT = 10;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ShipmentListReader reader;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
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

        var rows = reader.findShipments(PERIOD, scope(site), LIMIT);

        assertThat(rows).extracting("shipmentNo").containsExactly("SHP-1", "SHP-2", "SHP-3");
        var red = rows.get(1);
        assertThat(red.hadDiscrepancy()).isTrue();
        assertThat(red.comparison().band()).isEqualTo("RED");
        assertThat(red.comparison().diffKg()).isEqualByComparingTo("-40.000");
        assertThat(rows.get(2).comparison().band()).isNull();
        assertThat(rows.get(2).completedAt()).isNull();
        assertThat(reader.findShipments(PERIOD, scope(site), 2)).hasSize(2);
        assertThat(reader.findShipments(PERIOD, new ReportScope(Set.of(), false, null), LIMIT))
                .isEmpty();
    }

    @Test
    @DisplayName("Fasoncu bakiyesi iptalleri saymaz; teslimi olmayan çıkışı yolda kalan olarak ayırır")
    void shouldSummarizeSubcontractorBalance() {
        UUID site = UUID.randomUUID();
        UUID subcontractor = UUID.randomUUID();
        SeedRows seed = new SeedRows(jdbc);
        seed.balanceShipment(site, new Balance(subcontractor, "COMPLETED", "1000.000", "990.000"));
        seed.balanceShipment(site, new Balance(subcontractor, "IN_TRANSIT", "500.000", null));
        seed.balanceShipment(site, new Balance(subcontractor, "CANCELLED", "300.000", null));

        var balances = reader.findSubcontractorBalances(PERIOD, scope(site));

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
