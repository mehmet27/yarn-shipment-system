-- İkinci dalga birleştirme: central_production (merkez fabrikanın ürettiği kumaş) ile fabric_receipt (fasoncudan gelen
-- kumaş) aynı şekle sahiptir. Tek `fabric_lot` tablosu olur: source = 'SUBCONTRACTOR' (shipment_id dolu) ya da
-- 'CENTRAL' (shipment_id boş, fire_rate dolu). fabric_receipt tablosu yeniden adlandırılır, veri ve id'ler korunur.

ALTER TABLE fabric_receipt RENAME TO fabric_lot;
ALTER INDEX fabric_receipt_pkey RENAME TO fabric_lot_pkey;
ALTER INDEX fabric_receipt_shipment_idx RENAME TO fabric_lot_shipment_idx;
ALTER TRIGGER fabric_receipt_append_only ON fabric_lot RENAME TO fabric_lot_append_only;
ALTER TABLE fabric_lot RENAME CONSTRAINT fabric_receipt_yarns_array TO fabric_lot_yarns_array;
ALTER TABLE fabric_lot RENAME COLUMN received_at TO occurred_at;

ALTER TABLE fabric_lot
    ADD COLUMN source    text          NOT NULL DEFAULT 'SUBCONTRACTOR' CHECK (source IN ('SUBCONTRACTOR', 'CENTRAL')),
    ADD COLUMN fire_rate numeric(5, 4) CHECK (fire_rate >= 0 AND fire_rate <= 0.2),
    ALTER COLUMN shipment_id DROP NOT NULL,
    ALTER COLUMN waybill_no DROP NOT NULL;
ALTER TABLE fabric_lot ALTER COLUMN source DROP DEFAULT;

-- Eski kısıtlar: fasoncu kaydında sevkiyat ve irsaliye zorunlu, merkez kaydında fire oranı zorunlu.
ALTER TABLE fabric_lot ADD CONSTRAINT fabric_lot_source_fields CHECK (
    (source = 'SUBCONTRACTOR' AND shipment_id IS NOT NULL AND waybill_no IS NOT NULL AND fire_rate IS NULL)
    OR (source = 'CENTRAL' AND shipment_id IS NULL AND fire_rate IS NOT NULL));
ALTER TABLE fabric_lot ADD CONSTRAINT fabric_lot_waybill_not_blank CHECK (waybill_no IS NULL OR length(btrim(waybill_no)) > 0);
ALTER TABLE fabric_lot DROP CONSTRAINT IF EXISTS fabric_receipt_waybill_no_check;
ALTER TABLE fabric_lot DROP CONSTRAINT IF EXISTS fabric_receipt_roll_count_check;
ALTER TABLE fabric_lot ADD CONSTRAINT fabric_lot_roll_count_check CHECK (roll_count IS NULL OR roll_count > 0);

COMMENT ON TABLE fabric_lot IS 'Kumaş partisi: fasoncudan gelen (SUBCONTRACTOR) ve merkez fabrikanın ürettiği (CENTRAL); yalnız ekleme';
COMMENT ON COLUMN fabric_lot.source IS 'SUBCONTRACTOR: shipment_id dolu, fire oranı sevkiyatın fire dosyasındadır; CENTRAL: fire_rate dolu';

-- Merkez üretimini taşı (id korunur, central_stock_movement.production_id FK'si fabric_lot'a döner).
ALTER TABLE central_stock_movement DROP CONSTRAINT central_stock_movement_production_id_fkey;
INSERT INTO fabric_lot (id, source, shipment_id, fabric_type, net_kg, fire_rate, roll_count, waybill_no, occurred_at,
                        created_at, created_by, yarns)
SELECT id, 'CENTRAL', NULL, fabric_type, net_kg, fire_rate, roll_count, waybill_no, produced_at, produced_at, created_by,
       '[]'::jsonb
FROM central_production;
ALTER TABLE central_stock_movement
    ADD CONSTRAINT central_stock_movement_production_id_fkey FOREIGN KEY (production_id) REFERENCES fabric_lot (id);

DROP TABLE central_production;

CREATE INDEX fabric_lot_central_idx ON fabric_lot (occurred_at) WHERE source = 'CENTRAL';

-- Uyumluluk görünümü: reporting-service'in V202611161100 görünümleri shipment.fabric_receipt'i okur ve taze kurulumda
-- shipment geçmişi önce tamamlanırsa bu ad bulunmalıdır. Yalnız fasoncu kayıtlarını eski şekliyle verir; reporting
-- görünümleri fabric_lot'a çevrilince kaldırılabilir.
CREATE VIEW fabric_receipt AS
SELECT id, shipment_id, fabric_type, net_kg, roll_count, waybill_no, occurred_at AS received_at, created_at, created_by
FROM fabric_lot
WHERE source = 'SUBCONTRACTOR';

COMMENT ON VIEW fabric_receipt IS 'Geçici uyumluluk görünümü; yeni kod fabric_lot kullanır';
GRANT SELECT ON fabric_receipt TO ${app_role};
