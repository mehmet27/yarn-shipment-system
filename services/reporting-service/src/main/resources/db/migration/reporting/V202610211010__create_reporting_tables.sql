-- reporting_db: olaylardan türetilen okuma modelleri (veritabanı tasarımı, "audit_db, notification_db,
-- reporting_db"). Kayıt kaynağı değildir; silinip Kafka olaylarından yeniden oluşturulabilir.

CREATE TABLE shipment_view (
    shipment_id            uuid           PRIMARY KEY,
    site_id                uuid           NOT NULL,
    shipment_no            text           NOT NULL,
    subcontractor_id       uuid           NOT NULL,
    driver_id              uuid,
    status                 text           NOT NULL,
    dispatch_net_kg        numeric(12, 3),
    delivery_net_kg        numeric(12, 3),
    diff_pct               numeric(7, 2),
    band                   text           CHECK (band IN ('GREEN', 'YELLOW', 'RED')),
    open_discrepancy_count integer        NOT NULL DEFAULT 0 CHECK (open_discrepancy_count >= 0),
    completed_at           timestamptz,
    created_at             timestamptz    NOT NULL,
    updated_at             timestamptz,
    version                integer        NOT NULL DEFAULT 0
);

COMMENT ON TABLE shipment_view IS 'Dashboard okuma modeli; yalnızca iç kullanıcılara açılır (kör giriş)';

CREATE INDEX shipment_view_site_status_completed_idx ON shipment_view (site_id, status, completed_at);

CREATE TABLE pair_daily_stats (
    day               date           NOT NULL,
    driver_id         uuid           NOT NULL,
    subcontractor_id  uuid           NOT NULL,
    shipment_count    integer        NOT NULL DEFAULT 0 CHECK (shipment_count >= 0),
    discrepancy_count integer        NOT NULL DEFAULT 0 CHECK (discrepancy_count >= 0),
    total_diff_kg     numeric(14, 3) NOT NULL DEFAULT 0,
    created_at        timestamptz    NOT NULL,
    updated_at        timestamptz,
    version           integer        NOT NULL DEFAULT 0,
    PRIMARY KEY (day, driver_id, subcontractor_id)
);

COMMENT ON TABLE pair_daily_stats IS 'Şoför–fasoncu eşleşme raporu (rapor Bölüm 13)';
COMMENT ON COLUMN pair_daily_stats.day IS 'Europe/Istanbul takvim günü';

GRANT SELECT, INSERT, UPDATE ON shipment_view, pair_daily_stats TO ${app_role};
