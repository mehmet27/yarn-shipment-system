-- Yalnızca yerel geliştirme (local profili): ekranlar veri göstersin diye farklı durum ve lokasyonlarda
-- örnek sevkiyatlar. Kimlikler yerel Keycloak realm'indeki kullanıcılarla eşleşir (deploy/keycloak). Tekrarlanabilir
-- migration: dosya değişirse yeniden çalışır, kayıtlar ON CONFLICT ile tekrarlanmaz.
SELECT set_config('app.actor_id', '0192f0a0-0000-7000-8000-00000000d001', false);

-- Sabitler: lokasyon A/B, fasoncular (fasoncu kullanıcısı C1), depo kullanıcısı U1.
CREATE TEMP TABLE seed_shipment ON COMMIT DROP AS
SELECT *
FROM (VALUES
    -- n, id, site, no, subcontractor, title, driver, driver_name, plate, status, planned_kg, planned_bags, day
    (1, '0192f0a0-0000-7000-8000-0000000a0001'::uuid, '0192f0a0-0000-7000-8000-000000000001'::uuid,
     'SV-20261015-D0000001', '0192f0a0-0000-7000-8000-000000000003'::uuid, 'Örnek Tekstil A.Ş.',
     NULL::uuid, NULL::text, NULL::text, 'DRAFT', NULL::numeric, NULL::int, '2026-10-15T08:00:00Z'::timestamptz),
    (2, '0192f0a0-0000-7000-8000-0000000a0002', '0192f0a0-0000-7000-8000-000000000002',
     'SV-20261016-D0000002', '0192f0a0-0000-7000-8000-000000000013', 'Başka Dokuma Ltd.',
     '0192f0a0-0000-7000-8000-000000000014', 'Mehmet Kaya', '06KAY456', 'DRAFT', NULL, NULL,
     '2026-10-16T08:00:00Z'),
    (3, '0192f0a0-0000-7000-8000-0000000a0003', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261017-D0000003', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'AWAITING_DELIVERY', 250.000, 5,
     '2026-10-17T08:00:00Z'),
    (4, '0192f0a0-0000-7000-8000-0000000a0004', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261018-D0000004', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'AWAITING_DELIVERY', 250.000, 5,
     '2026-10-18T08:00:00Z'),
    (5, '0192f0a0-0000-7000-8000-0000000a0005', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261019-D0000005', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'AWAITING_DELIVERY', 250.000, 5,
     '2026-10-19T08:00:00Z'),
    (6, '0192f0a0-0000-7000-8000-0000000a0006', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261020-D0000006', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'COMPLETED', 250.000, 5,
     '2026-10-20T08:00:00Z'),
    (7, '0192f0a0-0000-7000-8000-0000000a0007', '0192f0a0-0000-7000-8000-000000000002',
     'SV-20261020-D0000007', '0192f0a0-0000-7000-8000-000000000013', 'Başka Dokuma Ltd.',
     '0192f0a0-0000-7000-8000-000000000014', 'Mehmet Kaya', '06KAY456', 'AWAITING_DELIVERY', 250.000, 5,
     '2026-10-20T10:00:00Z')
) AS t(n, id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id, driver_name, plate_no,
       status, planned_kg, planned_bags, created_at);

INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id, driver_name,
                      vehicle_id, plate_no, status, planned_net_kg, planned_bag_count, created_at, created_by)
SELECT id, site_id, shipment_no, subcontractor_id, subcontractor_title, driver_id, driver_name,
       CASE plate_no WHEN '34ABC123' THEN '0192f0a0-0000-7000-8000-000000000021'::uuid
                     WHEN '06KAY456' THEN '0192f0a0-0000-7000-8000-000000000022'::uuid END,
       plate_no, status,
       planned_kg, planned_bags, created_at, '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment
ON CONFLICT (id) DO NOTHING;

-- Kilitli (tamamlanmış) sevkiyatın alt kayıtları tekrar eklenmez: kilit tetikleyicisi ON CONFLICT'ten önce çalışır.
-- Çuvallar: atanmış (DRAFT dışı) sevkiyatlara beşer çuval; sıra no 1-5, her biri 50 kg (net 250 kg).
INSERT INTO shipment_bag (id, shipment_id, bag_id, unique_key, seq_no, weight_kg, scan_method,
                          assigned_at, assigned_by, created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000b' || lpad(s.n::text, 2, '0') || lpad(b::text, 2, '0'))::uuid,
       s.id,
       ('0192f0a0-0000-7000-8000-0000000c' || lpad(s.n::text, 2, '0') || lpad(b::text, 2, '0'))::uuid,
       b::text, b, 50.000, 'MANUAL',
       s.created_at, '0192f0a0-0000-7000-8000-00000000d001', s.created_at, '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment s CROSS JOIN generate_series(1, 5) AS b
WHERE s.status <> 'DRAFT'
  AND NOT EXISTS (SELECT 1 FROM shipment k WHERE k.id = s.id AND k.locked_at IS NOT NULL)
ON CONFLICT (id) DO NOTHING;

-- Çıkış tartımı: DRAFT dışındaki her sevkiyat. Brüt 253.500, dara 3.500, net 250.000, beş çuval.
INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                      created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000d' || lpad(n::text, 4, '0'))::uuid, id, 'DISPATCH', 253.500, 3.500, 5,
       created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001',
       created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment
WHERE status <> 'DRAFT'
  AND NOT EXISTS (SELECT 1 FROM shipment k WHERE k.id = seed_shipment.id AND k.locked_at IS NOT NULL)
ON CONFLICT (id) DO NOTHING;

-- Tamamlanan örnek: müdür irsaliye numarasını girdi, sevkiyat kilitlendi. Kilit en son konur; aksi halde bağlı
-- satırlar eklenemez.
UPDATE shipment sh SET e_waybill_no = 'IRS-SEED-' || s.n, locked_at = s.created_at + interval '2 days'
FROM seed_shipment s
WHERE sh.id = s.id AND s.status = 'COMPLETED' AND sh.status = 'COMPLETED' AND sh.locked_at IS NULL;

-- Durum geçmişi: DRAFT'tan bugünkü duruma kadar her adım.
INSERT INTO shipment_status_history (id, created_at, shipment_id, from_status, to_status, changed_by, changed_at)
SELECT ('0192f0a0-0000-7000-8000-00000010' || lpad(s.n::text, 2, '0') || lpad(h.step::text, 2, '0'))::uuid,
       s.created_at + (h.step * interval '1 hour'), s.id, h.from_status, h.to_status,
       '0192f0a0-0000-7000-8000-00000000d001', s.created_at + (h.step * interval '1 hour')
FROM seed_shipment s
JOIN (VALUES (0, NULL, 'DRAFT'), (1, 'DRAFT', 'AWAITING_DELIVERY'),
             (2, 'AWAITING_DELIVERY', 'COMPLETED')) AS h(step, from_status, to_status)
  ON h.step <= CASE s.status WHEN 'DRAFT' THEN 0 WHEN 'AWAITING_DELIVERY' THEN 1 ELSE 2 END
ON CONFLICT (id, created_at) DO NOTHING;

-- Ana veri: sevkiyat örneklerinin başvurduğu kayıtlar ve seçim kutularında liste dolsun diye birkaç ek kayıt.
INSERT INTO md_site (id, name, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000001', 'Merkez Fabrika', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000002', 'Çorlu Depo', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

-- Firmanın tek lokasyonu Merkez Fabrika'dır; ekranlar lokasyon seçtirmez. Çorlu Depo yalnızca eski örnek
-- kayıtların (sevkiyat, parti) lokasyon bağı için durur, yeni kayıt açılamaz ve hesaplara atanmaz.
UPDATE md_site SET active = false WHERE id = '0192f0a0-0000-7000-8000-000000000002' AND active;

INSERT INTO md_subcontractor (id, title, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000013', 'Başka Dokuma Ltd.', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000023', 'Güneş Boyahanesi', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO md_driver (id, full_name, subcontractor_id, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '0192f0a0-0000-7000-8000-000000000003', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000014', 'Mehmet Kaya', '0192f0a0-0000-7000-8000-000000000013', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000024', 'Ayşe Demir', '0192f0a0-0000-7000-8000-000000000003', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO md_vehicle (id, plate_no, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000021', '34ABC123', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000022', '06KAY456', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000025', '35IZM789', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

-- İplik, kalite ve parti: bag-service örnek çuvallarının partileriyle aynı kimlikler (kilitli); bir de kilitsiz parti.
INSERT INTO md_product (id, code, name, yarn_count, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000201', 'PES-30-1', 'Polyester 30/1', 'Ne 30/1', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000202', 'CO-20-1', 'Pamuk 20/1', 'Ne 20/1', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO md_quality_grade (id, code, name, sort_order, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000211', 'A', 'Birinci kalite', 1, '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000212', 'B', 'İkinci kalite', 2, '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id, production_date, quality_locked_at,
                               quality_locked_by, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000101', '0192f0a0-0000-7000-8000-000000000001', 'P-2026-0001',
        '0192f0a0-0000-7000-8000-000000000201', '0192f0a0-0000-7000-8000-000000000211', '2026-10-10',
        '2026-10-11T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001', '2026-10-10T08:00:00Z',
        '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000102', '0192f0a0-0000-7000-8000-000000000002', 'P-2026-0002',
        '0192f0a0-0000-7000-8000-000000000201', '0192f0a0-0000-7000-8000-000000000212', '2026-10-12',
        '2026-10-13T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001', '2026-10-12T08:00:00Z',
        '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000103', '0192f0a0-0000-7000-8000-000000000001', 'P-2026-0003',
        '0192f0a0-0000-7000-8000-000000000202', '0192f0a0-0000-7000-8000-000000000211', '2026-10-20', NULL, NULL,
        '2026-10-20T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

-- İplik tedarikçileri (süreç analizi v2, Bölüm 5.1).
INSERT INTO md_yarn_supplier (id, name, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000301', 'Şireci', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000302', 'Karesi', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000303', 'Teksmar', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000304', 'Migiteks', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000305', 'Creora', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

-- Tamamlanmış örnek sevkiyatlar için, gerçek akıştaki (CompleteShipmentService) gibi çıkış defter hareketi ve fire dosyası.
-- Çıkış hareketi yazılmazsa fasoncu bakiyesi 0 kalır; fire dosyası yazılmazsa tamamlanan sevkiyatın fire sekmesi boş çıkar.
-- Yolda olan (AWAITING_DELIVERY) sevkiyatın kilosu bakiyeye girmez, "yolda" olarak ayrı hesaplanır.
INSERT INTO balance_movement (id, subcontractor_id, yarn_type, shipment_id, kind, net_kg, occurred_at, created_by,
                              source_ref)
SELECT ('0192f0a0-0000-7000-8000-0000000e' || lpad(s.n::text, 4, '0'))::uuid, s.subcontractor_id, '(reçetesiz)',
       s.id, 'DISPATCH', 250.000, s.created_at + interval '2 days', '0192f0a0-0000-7000-8000-00000000d001',
       'DISPATCH:' || s.id || ':1'
FROM seed_shipment s
WHERE s.status = 'COMPLETED'
ON CONFLICT (source_ref) DO NOTHING;

INSERT INTO fire_settlement (shipment_id, subcontractor_id, status, fire_rate, sent_kg, created_at)
SELECT s.id, s.subcontractor_id, 'AWAITING_FABRIC', 0.0400, 250.000, s.created_at + interval '2 days'
FROM seed_shipment s
WHERE s.status = 'COMPLETED'
ON CONFLICT (shipment_id) DO NOTHING;
