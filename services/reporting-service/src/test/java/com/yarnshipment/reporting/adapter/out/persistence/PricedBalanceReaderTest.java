package com.yarnshipment.reporting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.reporting.application.port.out.BalanceFirePort.PricedBalance;
import com.yarnshipment.reporting.application.port.out.ReportScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Fasoncudaki ipliğin tutarı için son fiyat: o fasoncuya o iplik için en son açılan sevkiyatın fiyatıdır. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BalanceFireReader.class)
class PricedBalanceReaderTest {

    private static final UUID SITE = UUID.randomUUID();
    private static final String YARN = "30/1 PENYE";
    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment.shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                           created_at)
            VALUES (?, ?, ?, ?, 'Fason', 'COMPLETED', ?::timestamptz)""";
    private static final String INSERT_LINE = """
            INSERT INTO shipment.shipment_line (id, shipment_id, line_no, yarn_type_text, net_kg, unit_price)
            VALUES (gen_random_uuid(), ?, ?, ?, 100, ?::numeric)""";
    private static final String INSERT_MOVEMENT = """
            INSERT INTO shipment.balance_movement (id, subcontractor_id, yarn_type, shipment_id, kind, net_kg,
                                                   occurred_at, created_by)
            VALUES (gen_random_uuid(), ?, ?, ?, 'DISPATCH', ?::numeric, now(), gen_random_uuid())""";

    @Autowired
    private BalanceFireReader reader;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    private UUID shipment(UUID subcontractor, String createdAt, String yarn, @Nullable String price) {
        UUID id = UUID.randomUUID();
        jdbc.update(INSERT_SHIPMENT, id, SITE, "PB-" + id, subcontractor, createdAt);
        jdbc.update(INSERT_LINE, id, 1, yarn, price);
        jdbc.update(INSERT_MOVEMENT, subcontractor, yarn, id, "100");
        return id;
    }

    private List<PricedBalance> balancesOf(UUID subcontractor) {
        return reader.findPricedBalances(new ReportScope(Set.of(), true, null)).stream()
                .filter(row -> row.subcontractorId().equals(subcontractor))
                .toList();
    }

    @Test
    @DisplayName("Aynı iplik için birden çok fiyat varsa en son açılan sevkiyatın fiyatı gelir; bakiye toplanır")
    void shouldUseLatestPricePerSubcontractorAndYarn() {
        UUID subcontractor = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        shipment(subcontractor, "2026-11-01T09:00:00Z", YARN, "40.00");
        shipment(subcontractor, "2026-11-05T09:00:00Z", YARN, "45.50");
        shipment(subcontractor, "2026-11-03T09:00:00Z", YARN, "42.00");
        // Başka fasoncunun daha yeni fiyatı bu fasoncuya karışmaz.
        shipment(other, "2026-11-09T09:00:00Z", YARN, "99.00");

        assertThat(balancesOf(subcontractor)).singleElement().satisfies(row -> {
            assertThat(row.balanceKg()).isEqualByComparingTo("300");
            assertThat(row.unitPrice()).isEqualByComparingTo("45.50");
        });
        assertThat(balancesOf(other))
                .singleElement()
                .satisfies(row -> assertThat(row.unitPrice()).isEqualByComparingTo("99.00"));
    }

    @Test
    @DisplayName("Hiç fiyat girilmemiş iplik fiyatsız (null) gelir; en son sevkiyat fiyatsızsa önceki fiyat kullanılır")
    void shouldLeavePriceEmptyWhenNeverPriced() {
        UUID subcontractor = UUID.randomUUID();
        shipment(subcontractor, "2026-11-01T09:00:00Z", "20/1 OE", null);
        shipment(subcontractor, "2026-11-01T09:00:00Z", YARN, "40.00");
        shipment(subcontractor, "2026-11-05T09:00:00Z", YARN, null);

        List<PricedBalance> rows = balancesOf(subcontractor);

        assertThat(rows).hasSize(2);
        assertThat(rows).filteredOn(row -> row.unitPrice() == null).hasSize(1);
        assertThat(rows)
                .filteredOn(row -> row.unitPrice() != null)
                .singleElement()
                .satisfies(row -> assertThat(row.balanceKg()).isEqualByComparingTo("200"));
    }
}
