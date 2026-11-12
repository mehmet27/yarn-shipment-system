-- İkinci dalga birleştirme: kumaş girişinin iplik dağılımı (fabric_receipt_yarn) ayrı tablo yerine fabric_receipt
-- üzerinde jsonb dizisidir: [{"yarnType": "...", "netKg": 12.500}, ...]. Kayıtlar yalnız eklendiği için tek satırda kalır.

ALTER TABLE fabric_receipt ADD COLUMN yarns jsonb NOT NULL DEFAULT '[]'::jsonb
    CONSTRAINT fabric_receipt_yarns_array CHECK (jsonb_typeof(yarns) = 'array');

COMMENT ON COLUMN fabric_receipt.yarns IS 'İplik dağılımı: [{"yarnType": text, "netKg": sayı}]; bakiyeden düşen hareketler balance_movement''a ayrıca yazılır';

-- fabric_receipt yalnız ekleme kabul ediyor; taşıma için trigger geçici kapatılır.
ALTER TABLE fabric_receipt DISABLE TRIGGER fabric_receipt_append_only;
UPDATE fabric_receipt r
SET yarns = coalesce((SELECT jsonb_agg(jsonb_build_object('yarnType', y.yarn_type, 'netKg', y.net_kg)
                                       ORDER BY y.yarn_type)
                      FROM fabric_receipt_yarn y WHERE y.fabric_receipt_id = r.id), '[]'::jsonb);
ALTER TABLE fabric_receipt ENABLE TRIGGER fabric_receipt_append_only;

DROP TABLE fabric_receipt_yarn;
