-- Dashboard ve raporlar için okuma modeli genişletmesi. Tüm tablolar olaylardan yeniden kurulabilir.

ALTER TABLE shipment_view
    ADD COLUMN diff_kg     numeric(12, 3),
    ADD COLUMN compared_at timestamptz,
    ADD COLUMN dispatched_at timestamptz,
    ADD COLUMN had_discrepancy boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN shipment_view.diff_kg IS 'Çıkış net − teslim net (karşılaştırma olayından); işaretlidir';
COMMENT ON COLUMN shipment_view.status IS 'Sevkiyat durumu: DRAFT.. CANCELLED (shipment-service ShipmentStatus)';

CREATE INDEX shipment_view_created_idx ON shipment_view (created_at);
CREATE INDEX shipment_view_driver_idx ON shipment_view (driver_id) WHERE driver_id IS NOT NULL;

-- Eşleşme raporunda uyuşmazlık oranı sevkiyat bazındadır; bir sevkiyatın birden çok uyuşmazlığı oranı şişirmez.
-- Tablo henüz hiçbir kod tarafından yazılmadı (boş); lokasyon kapsamı için anahtara site_id eklenir (SEC-03).
ALTER TABLE pair_daily_stats
    ADD COLUMN site_id uuid NOT NULL,
    ADD COLUMN discrepant_shipment_count integer NOT NULL DEFAULT 0 CHECK (discrepant_shipment_count >= 0);
ALTER TABLE pair_daily_stats DROP CONSTRAINT pair_daily_stats_pkey;
ALTER TABLE pair_daily_stats ADD PRIMARY KEY (day, site_id, driver_id, subcontractor_id);

COMMENT ON COLUMN pair_daily_stats.total_diff_kg IS 'Karşılaştırılan sevkiyatların mutlak kilo farkı toplamı';

CREATE TABLE discrepancy_view (
    discrepancy_id uuid        PRIMARY KEY,
    discrepancy_no text        NOT NULL,
    shipment_id    uuid        NOT NULL,
    site_id        uuid        NOT NULL,
    type           text        NOT NULL,
    severity       text        NOT NULL CHECK (severity IN ('YELLOW', 'RED')),
    due_at         timestamptz NOT NULL,
    resolved_at    timestamptz,
    created_at     timestamptz NOT NULL
);

COMMENT ON TABLE discrepancy_view IS 'Açık ve süresi geçen uyuşmazlık göstergeleri için okuma modeli';

CREATE INDEX discrepancy_view_open_due_idx ON discrepancy_view (due_at) WHERE resolved_at IS NULL;
CREATE INDEX discrepancy_view_site_idx ON discrepancy_view (site_id);

GRANT SELECT, INSERT, UPDATE ON discrepancy_view TO ${app_role};
