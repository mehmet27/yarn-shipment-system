package com.yarnshipment.app;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Aylık bölümlü tabloların ileriki aylarını açar. Migration'lar yalnızca ilk 15 ayı (2027-12'ye kadar) açar; bu iş
 * çalışmazsa o tarihten sonraki satırlar yedek ({@code _default}) bölüme düşer.
 *
 * <p>Açılışta ve her gece çalışır; her şemanın kendi {@code create_monthly_partitions()} fonksiyonunu çağırır
 * (var olan bölümü yeniden açmaz). Bir tablodaki hata diğerlerini durdurmaz, ERROR olarak loglanır (LOG-04).
 */
@Component
final class PartitionMaintenance {

    /** İçinde bulunulan aya ek olarak açık tutulan ay sayısı. */
    static final int MONTHS_AHEAD = 12;

    private static final Logger LOG = LoggerFactory.getLogger(PartitionMaintenance.class);
    private static final List<Partitioned> TABLES = List.of(
            new Partitioned("platform", "audit_local"),
            new Partitioned("shipment", "shipment_status_history"),
            new Partitioned("discrepancy", "discrepancy_activity"),
            new Partitioned("evidence", "evidence_file"),
            new Partitioned("audit", "audit_log"));

    private final JdbcClient jdbc;
    private final Clock clock;

    PartitionMaintenance(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${yarnshipment.database.partition-maintenance.cron:0 30 2 * * *}", zone = "UTC")
    void ensureUpcomingPartitions() {
        ensurePartitions(LocalDate.now(clock).withDayOfMonth(1), MONTHS_AHEAD + 1);
    }

    /**
     * @param fromMonth ilk bölümün ayı (ayın herhangi bir günü)
     * @param monthCount açılacak ay sayısı
     */
    void ensurePartitions(LocalDate fromMonth, int monthCount) {
        for (Partitioned table : TABLES) {
            try {
                jdbc.sql("SELECT " + table.schema()
                                + ".create_monthly_partitions(CAST(:parent AS regclass), :fromMonth, :monthCount)")
                        .param("parent", table.schema() + "." + table.name())
                        .param("fromMonth", fromMonth)
                        .param("monthCount", monthCount)
                        .query((rs, rowNum) -> rowNum)
                        .list();
            } catch (DataAccessException e) {
                LOG.error("Monthly partitions could not be created table={}.{}", table.schema(), table.name(), e);
            }
        }
    }

    private record Partitioned(String schema, String name) {}
}
