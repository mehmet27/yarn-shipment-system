package com.yarnshipment.reporting.adapter.out.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Sorgu testlerinin okuma modeli satırlarını doğrudan yazan yardımcısı. */
final class SeedRows {

    private static final UUID SUBCONTRACTOR = UUID.randomUUID();

    private static final String INSERT_DISCREPANCY = """
            INSERT INTO discrepancy.discrepancy (id, discrepancy_no, site_id, shipment_id, discrepancy_type, severity,
                                                 status, due_at, resolved_at, created_at)
            VALUES (gen_random_uuid(), ?, ?, ?, 'QUALITY', ?, ?, ?::timestamptz,
                    CASE WHEN ? THEN '2026-11-02T00:00:00Z'::timestamptz END, '2026-10-30T00:00:00Z')""";

    private static final String INSERT_SHIPMENT = """
            INSERT INTO shipment.shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                           locked_at, created_at)
            VALUES (?, ?, ?, ?, 'Fason', ?, CASE WHEN ? = 'COMPLETED' THEN '2026-11-03T10:00:00Z'::timestamptz END,
                    '2026-11-02T09:00:00Z')""";

    private static final String INSERT_WEIGHING = """
            INSERT INTO shipment.weighing (id, shipment_id, side, net_kg, weighed_at)
            VALUES (gen_random_uuid(), ?, 'DISPATCH', ?::numeric, '2026-11-02T08:00:00Z')""";

    private static final String INSERT_OLD_SHIPMENT = """
            INSERT INTO shipment.shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status,
                                           created_at)
            VALUES (gen_random_uuid(), ?, 'OLD', ?, 'Fason', 'COMPLETED', '2026-01-01T00:00:00Z')""";

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

    /** Sevkiyat, 100 kg çıkış tartımı ve (uyuşmazlıklıysa) çözülmüş bir uyuşmazlık yazar; görünüm bunlardan okur. */
    void shipment(UUID site, Shipment shipment) {
        UUID id = insertShipment(site, shipment.no(), new Balance(SUBCONTRACTOR, shipment.status(), "100"));
        if (shipment.discrepant()) {
            discrepancy(site, id, new Discrepancy("UY-" + shipment.no(), "YELLOW", "2026-11-09T00:00:00Z", true));
        }
    }

    private UUID insertShipment(UUID site, String no, Balance balance) {
        UUID id = UUID.randomUUID();
        jdbc.update(INSERT_SHIPMENT, id, site, no, balance.subcontractor(), balance.status(), balance.status());
        jdbc.update(INSERT_WEIGHING, id, balance.dispatchKg());
        return id;
    }

    /**
     * Bakiye testi sevkiyatı.
     *
     * @param subcontractor fasoncu
     * @param status durum
     * @param dispatchKg çıkış net kilo; tamamlanan sevkiyatta teslim kilosu da budur
     */
    record Balance(UUID subcontractor, String status, String dispatchKg) {}

    void balanceShipment(UUID site, Balance balance) {
        insertShipment(site, "BAL-" + UUID.randomUUID(), balance);
    }

    void oldShipment(UUID site) {
        jdbc.update(INSERT_OLD_SHIPMENT, site, SUBCONTRACTOR);
    }

    void discrepancy(UUID site, Discrepancy discrepancy) {
        discrepancy(site, UUID.randomUUID(), discrepancy);
    }

    private void discrepancy(UUID site, UUID shipmentId, Discrepancy discrepancy) {
        jdbc.update(
                INSERT_DISCREPANCY,
                discrepancy.no(),
                site,
                shipmentId,
                discrepancy.severity(),
                discrepancy.resolved() ? "RESOLVED" : "OPEN",
                discrepancy.dueAt(),
                discrepancy.resolved());
    }

    void subcontractor(UUID id, String title) {
        jdbc.update("INSERT INTO shipment.md_subcontractor (id, title) VALUES (?, ?)", id, title);
    }
}
