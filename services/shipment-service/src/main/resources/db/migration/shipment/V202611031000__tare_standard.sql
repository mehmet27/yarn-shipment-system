-- Standart dara tanımı ve çıkış tartımında sapma uyarısı (eksik listesi 1.8). Diğer ana veri gibi master-data servisi
-- gelene kadar shipment_db içinde `md_` önekiyle durur. Kayıt silinmez; lokasyon ve ambalaj tipi başına tek tanım vardır.

CREATE TABLE md_tare_standard (
    id                uuid           PRIMARY KEY,
    site_id           uuid           NOT NULL REFERENCES md_site (id),
    packaging_type    text           NOT NULL CHECK (packaging_type IN ('BAG', 'BOX', 'PALLET', 'BOBBIN')),
    standard_tare_kg  numeric(12, 3) NOT NULL CHECK (standard_tare_kg >= 0),
    max_deviation_pct numeric(5, 2)  NOT NULL CHECK (max_deviation_pct BETWEEN 0 AND 100),
    created_at        timestamptz    NOT NULL,
    created_by        uuid           NOT NULL,
    updated_at        timestamptz,
    updated_by        uuid,
    UNIQUE (site_id, packaging_type)
);

COMMENT ON TABLE md_tare_standard IS 'Standart dara; ambalaj başına kg. Beklenen dara = standart × çuval sayısı';

GRANT SELECT, INSERT, UPDATE ON md_tare_standard TO ${app_role};

CREATE TRIGGER md_tare_standard_audit_local
    AFTER INSERT OR UPDATE OR DELETE ON md_tare_standard
    FOR EACH ROW EXECUTE FUNCTION audit_local_capture();

-- Tartım, kaydedildiği andaki standardı kopyalar: standart sonradan değişse de eski tartımın sapması değişmez.
-- Standart tanımsızsa ikisi de boştur. Sapma ve uyarı okurken hesaplanır.
ALTER TABLE weighing
    ADD COLUMN expected_tare_kg   numeric(12, 3),
    ADD COLUMN tare_tolerance_pct numeric(5, 2),
    ADD CONSTRAINT weighing_tare_basis_complete
        CHECK ((expected_tare_kg IS NULL) = (tare_tolerance_pct IS NULL));

COMMENT ON COLUMN weighing.expected_tare_kg IS 'Kayıt anındaki standart dara × çuval sayısı';
COMMENT ON COLUMN weighing.tare_tolerance_pct IS 'Kayıt anındaki izin verilen en çok sapma (%)';
