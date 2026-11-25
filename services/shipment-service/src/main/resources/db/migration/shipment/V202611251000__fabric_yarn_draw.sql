-- Kumaş tek ipliklen ve tek sevkiyattan üretilmez: kalitecinin yazdığı orana göre fasoncunun elindeki tüm ipliklerden
-- düşer. Her düşüm bir satırdır: hangi kumaş kaydı, hangi sevkiyatın hangi ipliğinden (ve lotundan) kaç kg aldı.
-- Sevkiyatın fire dosyasındaki "gelen kumaş" artık bu düşümlerin toplamıdır (kumaşın girildiği sevkiyat değil,
-- ipliğinin düşüldüğü sevkiyat). Kayıtlar yalnız eklenir.
CREATE TABLE fabric_yarn_draw (
    id                 uuid           PRIMARY KEY,
    fabric_lot_id      uuid           NOT NULL REFERENCES fabric_lot (id),
    source_shipment_id uuid           NOT NULL REFERENCES shipment (id),
    yarn_type          text           NOT NULL CHECK (length(btrim(yarn_type)) > 0),
    lot_id             uuid,
    net_kg             numeric(12, 3) NOT NULL CHECK (net_kg > 0),
    position           integer        NOT NULL DEFAULT 0,
    created_at         timestamptz    NOT NULL DEFAULT now()
);

COMMENT ON TABLE fabric_yarn_draw IS 'Kumaşın tükettiği iplik: hangi kumaş kaydı hangi sevkiyatın hangi ipliğinden kaç kg düştü; yalnız ekleme';
COMMENT ON COLUMN fabric_yarn_draw.position IS 'Kumaş kaydı içindeki düşüm sırası: önce girildiği sevkiyat, sonra en eskiden yeniye';
COMMENT ON COLUMN fabric_yarn_draw.source_shipment_id IS 'İpliği düşülen sevkiyat; kumaşın girildiği sevkiyattan farklı olabilir';

CREATE INDEX fabric_yarn_draw_source_idx ON fabric_yarn_draw (source_shipment_id);
CREATE INDEX fabric_yarn_draw_lot_idx ON fabric_yarn_draw (fabric_lot_id);

CREATE TRIGGER fabric_yarn_draw_append_only
    BEFORE UPDATE OR DELETE ON fabric_yarn_draw
    FOR EACH ROW EXECUTE FUNCTION reject_modification();

GRANT SELECT, INSERT ON fabric_yarn_draw TO ${app_role};

-- Eski kumaş kayıtları kendi sevkiyatlarının ipliğinden düşmüştü; aynı bilgi düşüm satırı olarak taşınır.
INSERT INTO fabric_yarn_draw (id, fabric_lot_id, source_shipment_id, yarn_type, lot_id, net_kg, created_at)
SELECT gen_random_uuid(), l.id, l.shipment_id, y."yarnType", NULL, y."netKg", l.created_at
FROM fabric_lot l
CROSS JOIN LATERAL jsonb_to_recordset(l.yarns) AS y("yarnType" text, "netKg" numeric)
WHERE l.source = 'SUBCONTRACTOR' AND l.shipment_id IS NOT NULL AND y."yarnType" IS NOT NULL AND y."netKg" > 0;
