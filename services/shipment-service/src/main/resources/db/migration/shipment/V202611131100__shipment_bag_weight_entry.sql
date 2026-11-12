-- Çuval girişi sıra no + kg olur (akış dokümanı S3): çuval anahtarı, QR ve bag-service rezervasyonu kullanılmaz.
-- Mevcut satırlar sevkiyat içinde atama sırasıyla numaralanır; kilosu bilinmediği için 0 yazılır.

ALTER TABLE shipment_bag
    ADD COLUMN seq_no integer,
    ADD COLUMN weight_kg numeric(12, 3),
    ALTER COLUMN lot_no DROP NOT NULL,
    ALTER COLUMN quality_grade_code DROP NOT NULL;

ALTER TABLE shipment_bag DISABLE TRIGGER USER;
UPDATE shipment_bag b
   SET seq_no = numbered.seq, weight_kg = 0
  FROM (SELECT id, row_number() OVER (PARTITION BY shipment_id ORDER BY assigned_at, id) AS seq
          FROM shipment_bag) numbered
 WHERE b.id = numbered.id;
ALTER TABLE shipment_bag ENABLE TRIGGER USER;

ALTER TABLE shipment_bag
    ALTER COLUMN seq_no SET NOT NULL,
    ALTER COLUMN weight_kg SET NOT NULL,
    ADD CONSTRAINT shipment_bag_seq_positive CHECK (seq_no > 0),
    ADD CONSTRAINT shipment_bag_weight_positive CHECK (weight_kg >= 0);

-- Çıkarılan sıra numarası yeniden kullanılabilir; benzersizlik yalnızca aktif satırlar için geçerlidir.
CREATE UNIQUE INDEX shipment_bag_seq_active_uq ON shipment_bag (shipment_id, seq_no) WHERE removed_at IS NULL;

COMMENT ON COLUMN shipment_bag.seq_no IS 'Sevkiyat içindeki çuval sırası (1, 2, 3...); unique_key bunun metin halidir';
