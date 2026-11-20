package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Aylık bölümlü beş tablonun ileriki ayları açılır ve hiçbir tarih bölümsüz kalmaz. */
@SpringBootTest(
        properties = {
            "yarnshipment.evidence.storage.signing-key=test-signing-key-test-signing-key-0123",
            "yarnshipment.evidence.storage.root=target/evidence-test"
        })
class PartitionMaintenanceTest {

    private static final DateTimeFormatter SUFFIX = DateTimeFormatter.ofPattern("yyyyMM");
    /** Migration'ların açtığı son ayın (2027-12) çok ötesinde bir ay. */
    private static final LocalDate FAR_MONTH = LocalDate.parse("2031-01-15");

    private static final int FAR_MONTH_COUNT = 3;
    private static final List<String> TABLES = List.of(
            "platform.audit_local",
            "shipment.shipment_status_history",
            "discrepancy.discrepancy_activity",
            "evidence.evidence_file",
            "audit.audit_log");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PartitionMaintenance maintenance;

    @Autowired
    private Clock clock;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Açılışta içinde bulunulan aydan 12 ay sonrasına kadar bölüm vardır")
    void shouldHavePartitionsForTheComingYearAfterStartup() {
        YearMonth last = YearMonth.now(clock).plusMonths(PartitionMaintenance.MONTHS_AHEAD);

        for (String table : TABLES) {
            assertThat(partitionExists(table + "_p" + last.format(SUFFIX)))
                    .as(table)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Migration'ların açtığı son aydan (2027-12) sonrası için de bölüm açar; yeniden çağrı zararsızdır")
    void shouldCreatePartitionsBeyondTheMigratedRange() {
        maintenance.ensurePartitions(FAR_MONTH, FAR_MONTH_COUNT);
        maintenance.ensurePartitions(FAR_MONTH.withDayOfMonth(1), FAR_MONTH_COUNT);

        for (String table : TABLES) {
            assertThat(partitionExists(table + "_p203101")).as(table).isTrue();
            assertThat(partitionExists(table + "_p203103")).as(table).isTrue();
            assertThat(partitionExists(table + "_p203104")).as(table).isFalse();
        }
    }

    @Test
    @DisplayName("Her bölümlü tablonun yedek bölümü vardır: bölümü açılmamış tarih de yazılabilir")
    void shouldHaveDefaultPartitionOnEveryPartitionedTable() {
        List<@Nullable String> withoutDefault = jdbc.queryForList("""
                SELECT n.nspname || '.' || c.relname
                  FROM pg_partitioned_table p
                  JOIN pg_class c ON c.oid = p.partrelid
                  JOIN pg_namespace n ON n.oid = c.relnamespace
                 WHERE p.partdefid = 0
                 ORDER BY 1""", String.class);

        assertThat(withoutDefault).isEmpty();
        assertThat(TABLES)
                .allSatisfy(
                        table -> assertThat(partitionExists(table + "_default")).isTrue());
    }

    private boolean partitionExists(String qualifiedName) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, qualifiedName));
    }
}
