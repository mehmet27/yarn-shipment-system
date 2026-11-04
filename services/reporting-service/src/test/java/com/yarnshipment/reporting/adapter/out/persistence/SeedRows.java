package com.yarnshipment.reporting.adapter.out.persistence;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;

/** Sorgu testlerinin okuma modeli satırlarını doğrudan yazan yardımcısı. */
final class SeedRows {

    private static final UUID SUBCONTRACTOR = UUID.randomUUID();

    private static final String INSERT_DISCREPANCY = """
            INSERT INTO discrepancy_view (discrepancy_id, discrepancy_no, shipment_id, site_id, type, severity,
                                          due_at, resolved_at, created_at)
            VALUES (gen_random_uuid(), ?, gen_random_uuid(), ?, 'QUANTITY', ?, ?::timestamptz,
                    CASE WHEN ? THEN '2026-11-02T00:00:00Z'::timestamptz END, '2026-10-30T00:00:00Z')""";

    private final JdbcTemplate jdbc;

    SeedRows(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Sevkiyat satırı.
     *
     * @param no sevkiyat numarası
     * @param driver şoför
     * @param status durum
     * @param band karşılaştırma bandı; karşılaştırılmadıysa boş
     * @param diffKg işaretli kilo farkı; karşılaştırılmadıysa boş
     * @param discrepant uyuşmazlık çıktı mı
     */
    record Shipment(
            String no,
            UUID driver,
            String status,
            @Nullable String band,
            @Nullable String diffKg,
            boolean discrepant) {}

    /**
     * Uyuşmazlık satırı.
     *
     * @param no numara
     * @param severity öncelik
     * @param dueAt vade, ISO 8601
     * @param resolved çözüldü mü
     */
    record Discrepancy(String no, String severity, String dueAt, boolean resolved) {}

    /**
     * Günlük eşleşme sayacı.
     *
     * @param day gün, ISO 8601
     * @param shipments sevkiyat
     * @param discrepant uyuşmazlıklı sevkiyat
     * @param discrepancies uyuşmazlık
     * @param diffKg mutlak fark toplamı
     */
    record Pair(String day, int shipments, int discrepant, int discrepancies, String diffKg) {}

    void shipment(UUID site, Shipment shipment) {
        jdbc.update(
                """
                INSERT INTO shipment_view (shipment_id, site_id, shipment_no, subcontractor_id, driver_id, status,
                                           band, diff_kg, had_discrepancy, dispatch_net_kg, delivery_net_kg,
                                           created_at)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?::numeric, ?, 100, 100, '2026-11-02T09:00:00Z')""",
                site,
                shipment.no(),
                SUBCONTRACTOR,
                shipment.driver(),
                shipment.status(),
                shipment.band(),
                shipment.diffKg(),
                shipment.discrepant());
    }

    /** Bakiye testi için fasoncu, durum ve çıkış/teslim kilosu belirlenebilen sevkiyat. */
    void balanceShipment(UUID site, UUID subcontractor, String status, String dispatchKg, @Nullable String deliveryKg) {
        jdbc.update("""
                INSERT INTO shipment_view (shipment_id, site_id, shipment_no, subcontractor_id, status,
                                           dispatch_net_kg, delivery_net_kg, created_at)
                VALUES (gen_random_uuid(), ?, 'BAL', ?, ?, ?::numeric, ?::numeric, '2026-11-02T09:00:00Z')""", site, subcontractor, status, dispatchKg, deliveryKg);
    }

    void oldShipment(UUID site) {
        jdbc.update("""
                INSERT INTO shipment_view (shipment_id, site_id, shipment_no, subcontractor_id, status, created_at)
                VALUES (gen_random_uuid(), ?, 'OLD', ?, 'COMPLETED', '2026-01-01T00:00:00Z')""", site, SUBCONTRACTOR);
    }

    void discrepancy(UUID site, Discrepancy discrepancy) {
        String sql = INSERT_DISCREPANCY;
        jdbc.update(sql, discrepancy.no(), site, discrepancy.severity(), discrepancy.dueAt(), discrepancy.resolved());
    }

    void pair(UUID site, UUID driver, Pair pair) {
        jdbc.update(
                """
                INSERT INTO pair_daily_stats (day, site_id, driver_id, subcontractor_id, shipment_count,
                                              discrepant_shipment_count, discrepancy_count, total_diff_kg, created_at)
                VALUES (?::date, ?, ?, ?, ?, ?, ?, ?::numeric, '2026-11-03T00:00:00Z')""",
                pair.day(),
                site,
                driver,
                SUBCONTRACTOR,
                pair.shipments(),
                pair.discrepant(),
                pair.discrepancies(),
                pair.diffKg());
    }
}
