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
     * @param status durum
     * @param discrepant uyuşmazlık çıktı mı
     */
    record Shipment(String no, String status, boolean discrepant) {}

    /**
     * Uyuşmazlık satırı.
     *
     * @param no numara
     * @param severity öncelik
     * @param dueAt vade, ISO 8601
     * @param resolved çözüldü mü
     */
    record Discrepancy(String no, String severity, String dueAt, boolean resolved) {}

    void shipment(UUID site, Shipment shipment) {
        jdbc.update("""
                INSERT INTO shipment_view (shipment_id, site_id, shipment_no, subcontractor_id, status,
                                           had_discrepancy, dispatch_net_kg, delivery_net_kg,
                                           dispatched_at, created_at)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 100, 100, '2026-11-02T08:00:00Z',
                        '2026-11-02T09:00:00Z')""", site, shipment.no(), SUBCONTRACTOR, shipment.status(), shipment.discrepant());
    }

    /**
     * Bakiye testi sevkiyatı.
     *
     * @param subcontractor fasoncu
     * @param status durum
     * @param dispatchKg çıkış net kilo
     * @param deliveryKg teslim net kilo; teslim edilmediyse boş
     */
    record Balance(
            UUID subcontractor,
            String status,
            String dispatchKg,
            @Nullable String deliveryKg) {}

    void balanceShipment(UUID site, Balance balance) {
        String sql = """
                INSERT INTO shipment_view (shipment_id, site_id, shipment_no, subcontractor_id, status,
                                           dispatch_net_kg, delivery_net_kg, created_at)
                VALUES (gen_random_uuid(), ?, 'BAL', ?, ?, ?::numeric, ?::numeric, '2026-11-02T09:00:00Z')""";
        jdbc.update(sql, site, balance.subcontractor(), balance.status(), balance.dispatchKg(), balance.deliveryKg());
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
}
