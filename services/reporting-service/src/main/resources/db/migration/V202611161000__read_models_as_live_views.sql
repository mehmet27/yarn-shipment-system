-- Okuma modelleri olay beslemeli kopya tablolar olmaktan çıkıp canlı SQL görünümü olur (modüler monolit geçişi, adım 7).
-- Kaynak: shipment ve discrepancy şemalarındaki kayıt tabloları. Okuma kodu aynı ad ve kolonlarla çalışmaya devam eder.
-- DİKKAT: shipment_view, discrepancy_view, balance_movement_view ve fire_file_view tablolarındaki veri silinir.
-- balance_movement_view'deki açılış (OPENING) satırları defterin kendisine (shipment.balance_movement) taşınır.
-- Görünümler shipment ve discrepancy şemalarına bağlıdır; bu migration'dan önce o servislerin migration'ları çalışmış olmalıdır.

DO $$
BEGIN
    IF to_regclass('shipment.shipment') IS NULL OR to_regclass('shipment.fire_settlement') IS NULL
            OR to_regclass('discrepancy.discrepancy') IS NULL THEN
        RAISE EXCEPTION 'shipment ve discrepancy şemaları hazır değil; önce shipment-service ve discrepancy-service migration''ları çalıştırılmalı';
    END IF;
END
$$;

-- 1. Açılış bakiyeleri defterde yoksa taşınır (yinelenen aktarım source_ref ile engellenir). created_by: sistem kullanıcısı yok,
-- bu yüzden NOT NULL kolonu için sıfır UUID kullanılır.
INSERT INTO shipment.balance_movement (id, subcontractor_id, yarn_type, kind, net_kg, occurred_at, created_by, source_ref)
SELECT v.id, v.subcontractor_id, v.yarn_type, 'OPENING', v.net_kg, v.occurred_at,
       '00000000-0000-0000-0000-000000000000', 'OPENING:' || v.subcontractor_id || ':' || v.yarn_type
FROM balance_movement_view v
WHERE v.kind = 'OPENING' AND v.net_kg <> 0
ON CONFLICT DO NOTHING;

DROP TABLE balance_movement_view;
DROP TABLE fire_file_view;
DROP TABLE discrepancy_view;
DROP TABLE shipment_view;

-- 2. Uyuşmazlık: açık olan, çözülmemiş (OPEN, IN_REVIEW) olandır; kapatılan her kayıt önce çözülmüş sayılır.
CREATE VIEW discrepancy_view AS
SELECT d.id AS discrepancy_id,
       d.discrepancy_no,
       d.shipment_id,
       d.site_id,
       d.discrepancy_type AS type,
       d.severity,
       d.due_at,
       d.resolved_at,
       d.created_at
FROM discrepancy.discrepancy d;

COMMENT ON VIEW discrepancy_view IS 'Açık ve süresi geçen uyuşmazlık göstergeleri; discrepancy.discrepancy üzerinde canlı görünüm';

-- 3. Sevkiyat. Çıkış neti geçerli (değiştirilmemiş) çıkış tartımıdır; DRAFT ve WEIGHT_MISMATCH sevkiyatta gösterilmez; WEIGHT_MISMATCH (çıkış olayı yayınlanmadığı için okuma modelinde
-- hep taslaktı) rapor sözleşmesinde DRAFT görünür. Tamamlanan sevkiyatta fasoncuya ulaşan kilo çıkış netidir. Tamamlanma zamanı kilit zamanıdır.
CREATE VIEW shipment_view AS
SELECT s.id AS shipment_id,
       s.site_id,
       s.shipment_no,
       s.subcontractor_id,
       CASE WHEN s.status = 'WEIGHT_MISMATCH' THEN 'DRAFT' ELSE s.status END AS status,
       CASE WHEN s.status NOT IN ('DRAFT', 'WEIGHT_MISMATCH') THEN w.net_kg END AS dispatch_net_kg,
       CASE WHEN s.status = 'COMPLETED' THEN w.net_kg END AS delivery_net_kg,
       coalesce(d.open_count, 0)::integer AS open_discrepancy_count,
       CASE WHEN s.status = 'COMPLETED' THEN s.locked_at END AS completed_at,
       s.created_at,
       s.updated_at,
       s.version,
       CASE WHEN s.status NOT IN ('DRAFT', 'WEIGHT_MISMATCH') THEN w.weighed_at END AS dispatched_at,
       coalesce(d.total, 0) > 0 AS had_discrepancy
FROM shipment.shipment s
LEFT JOIN shipment.weighing w
       ON w.shipment_id = s.id AND w.side = 'DISPATCH' AND w.superseded_by IS NULL
LEFT JOIN LATERAL (
    SELECT count(*) AS total, count(*) FILTER (WHERE x.status IN ('OPEN', 'IN_REVIEW')) AS open_count
    FROM discrepancy.discrepancy x
    WHERE x.shipment_id = s.id
) d ON true;

COMMENT ON VIEW shipment_view IS 'Dashboard ve rapor okuma modeli; yalnızca iç kullanıcılara açılır (kör giriş). shipment şeması üzerinde canlı görünüm';

-- 4. Bakiye defteri. Lokasyon sevkiyattan gelir; OPENING gibi sevkiyatsız hareketler lokasyondan bağımsızdır.
CREATE VIEW balance_movement_view AS
SELECT m.id,
       m.subcontractor_id,
       coalesce(sub.title, s.subcontractor_title, '') AS subcontractor_name,
       m.yarn_type,
       m.shipment_id,
       s.site_id,
       m.kind,
       m.net_kg,
       m.occurred_at
FROM shipment.balance_movement m
LEFT JOIN shipment.shipment s ON s.id = m.shipment_id
LEFT JOIN shipment.md_subcontractor sub ON sub.id = m.subcontractor_id;

COMMENT ON VIEW balance_movement_view IS 'Fasoncu bakiye defteri; net_kg işaretlidir, bakiye toplamdır. shipment.balance_movement üzerinde canlı görünüm';

-- 5. Fire dosyası: G gönderilen, D dönen iplik (yalnız RETURN yönü), K gelen kumaş toplamı. Kapalı ve faturalanmış dosya
-- rapor sözleşmesinde CLOSED görünür.
CREATE VIEW fire_file_view AS
SELECT f.shipment_id,
       s.shipment_no,
       f.subcontractor_id,
       coalesce(sub.title, s.subcontractor_title, '') AS subcontractor_name,
       s.site_id,
       f.sent_kg,
       coalesce(r.returned_kg, 0)::numeric(14, 3) AS returned_yarn_kg,
       coalesce(fr.fabric_kg, 0)::numeric(14, 3) AS fabric_kg,
       f.fire_rate,
       CASE WHEN f.status = 'INVOICED' THEN 'CLOSED' ELSE f.status END AS status,
       f.invoice_no,
       (f.invoiced_on::timestamp AT TIME ZONE 'Europe/Istanbul') AS invoiced_at,
       coalesce(s.locked_at, f.created_at) AS completed_at
FROM shipment.fire_settlement f
JOIN shipment.shipment s ON s.id = f.shipment_id
LEFT JOIN shipment.md_subcontractor sub ON sub.id = f.subcontractor_id
LEFT JOIN LATERAL (
    SELECT sum(y.net_kg) AS returned_kg
    FROM shipment.yarn_return y
    WHERE y.shipment_id = f.shipment_id AND y.direction = 'RETURN'
) r ON true
LEFT JOIN LATERAL (
    SELECT sum(c.net_kg) AS fabric_kg
    FROM shipment.fabric_receipt c
    WHERE c.shipment_id = f.shipment_id
) fr ON true;

COMMENT ON VIEW fire_file_view IS 'Sevkiyat bazında fire dosyası (G, D, K, f); hesaplar kodda yapılır. shipment şeması üzerinde canlı görünüm';

GRANT SELECT ON shipment_view, discrepancy_view, balance_movement_view, fire_file_view TO ${app_role};
