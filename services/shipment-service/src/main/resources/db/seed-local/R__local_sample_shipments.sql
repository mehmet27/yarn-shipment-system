-- Yalnızca yerel geliştirme (application-local.yml): ekranlar veri göstersin diye farklı durum ve lokasyonlarda
-- örnek sevkiyatlar. Kimlikler yerel Keycloak realm'indeki kullanıcılarla eşleşir (deploy/keycloak). Tekrarlanabilir
-- migration: dosya değişirse yeniden çalışır, kayıtlar ON CONFLICT ile tekrarlanmaz.
SELECT set_config('app.actor_id', '0192f0a0-0000-7000-8000-00000000d001', false);

-- Sabitler: lokasyon A/B, fasoncular (fasoncu kullanıcısı C1), şoför kullanıcısı D1, depo kullanıcısı U1.
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
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'LOADED', 250.000, 5,
     '2026-10-17T08:00:00Z'),
    (4, '0192f0a0-0000-7000-8000-0000000a0004', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261018-D0000004', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'IN_TRANSIT', 250.000, 5,
     '2026-10-18T08:00:00Z'),
    (5, '0192f0a0-0000-7000-8000-0000000a0005', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261019-D0000005', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'AWAITING_DELIVERY', 250.000, 5,
     '2026-10-19T08:00:00Z'),
    (6, '0192f0a0-0000-7000-8000-0000000a0006', '0192f0a0-0000-7000-8000-000000000001',
     'SV-20261020-D0000006', '0192f0a0-0000-7000-8000-000000000003', 'Örnek Tekstil A.Ş.',
     '0192f0a0-0000-7000-8000-000000000004', 'Ali Yılmaz', '34ABC123', 'CHECKING', 250.000, 5,
     '2026-10-20T08:00:00Z'),
    (7, '0192f0a0-0000-7000-8000-0000000a0007', '0192f0a0-0000-7000-8000-000000000002',
     'SV-20261020-D0000007', '0192f0a0-0000-7000-8000-000000000013', 'Başka Dokuma Ltd.',
     '0192f0a0-0000-7000-8000-000000000014', 'Mehmet Kaya', '06KAY456', 'LOADED', 250.000, 5,
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

-- Çuvallar: atanmış (DRAFT dışı) sevkiyatlara beşer çuval.
INSERT INTO shipment_bag (id, shipment_id, bag_id, unique_key, lot_no, quality_grade_code, scan_method,
                          assigned_at, assigned_by, created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000b' || lpad(s.n::text, 2, '0') || lpad(b::text, 2, '0'))::uuid,
       s.id,
       ('0192f0a0-0000-7000-8000-0000000c' || lpad(s.n::text, 2, '0') || lpad(b::text, 2, '0'))::uuid,
       'BAG-' || s.n || '-' || b, 'LOT-2610-' || s.n, 'A', 'SCAN',
       s.created_at, '0192f0a0-0000-7000-8000-00000000d001', s.created_at, '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment s CROSS JOIN generate_series(1, 5) AS b
WHERE s.status <> 'DRAFT'
ON CONFLICT (id) DO NOTHING;

-- Çıkış tartımı ve mührü: LOADED ve sonrası. Brüt 253.500, dara 3.500, net 250.000, beş çuval.
INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, scale_ticket_no, weighed_at, entered_by,
                      created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000d' || lpad(n::text, 4, '0'))::uuid, id, 'DISPATCH', 253.500, 3.500, 5,
       'KF-' || n, created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001',
       created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment
WHERE status <> 'DRAFT'
ON CONFLICT (id) DO NOTHING;

INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by, recorded_at, created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000e' || lpad(n::text, 4, '0'))::uuid, id, 'DISPATCH', 'M-SEED-' || n,
       'INTACT', '0192f0a0-0000-7000-8000-00000000d001', created_at + interval '2 hours',
       created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment
WHERE status <> 'DRAFT'
ON CONFLICT (id) DO NOTHING;

-- Şoför beyanları: yola çıkan ve sonrası (PICKUP), teslimata varan ve sonrası (ARRIVAL).
INSERT INTO driver_declaration (id, shipment_id, declaration_type, driver_id, bag_count, seal_no, seal_intact,
                                declared_at, created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000f' || lpad(n::text, 2, '0') || CASE t WHEN 'PICKUP' THEN '01' ELSE '02' END)::uuid,
       id, t, driver_id, 5, 'M-SEED-' || n, true,
       created_at + interval '3 hours', created_at + interval '3 hours', driver_id
FROM seed_shipment CROSS JOIN (VALUES ('PICKUP'), ('ARRIVAL')) AS d(t)
WHERE (t = 'PICKUP' AND status IN ('IN_TRANSIT', 'AWAITING_DELIVERY', 'CHECKING'))
   OR (t = 'ARRIVAL' AND status IN ('AWAITING_DELIVERY', 'CHECKING'))
ON CONFLICT (id) DO NOTHING;

-- Teslim: kontrol aşamasındaki sevkiyatta fasoncunun gönderdiği alıcı kaydı, tartımı ve mührü.
INSERT INTO delivery_receipt (id, shipment_id, received_by, received_at, doc_shipment_no, doc_date, doc_net_kg,
                              doc_bag_count, created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000a' || lpad(n::text, 4, '0'))::uuid, id,
       '0192f0a0-0000-7000-8000-00000000c101', created_at + interval '6 hours', shipment_no,
       (created_at + interval '6 hours')::date, 249.000, 5,
       created_at + interval '6 hours', '0192f0a0-0000-7000-8000-00000000c101'
FROM seed_shipment
WHERE status = 'CHECKING'
ON CONFLICT (id) DO NOTHING;

-- Okutmalar: dört anahtar çıkış listesindeki çuvallarla eşleşir, biri listede yoktur. Teslim gönderilince okutma
-- eklenemez; bu yüzden önce okutmalar, sonra gönderim. Gönderilmiş kayıt yeniden çalıştırmada atlanır.
INSERT INTO delivery_scan (id, delivery_receipt_id, scanned_key, matched, scan_method, scanned_at, created_at,
                           created_by)
SELECT ('0192f0a0-0000-7000-8000-0000002f' || lpad(s.n::text, 2, '0') || lpad(k::text, 2, '0'))::uuid,
       r.id, CASE WHEN k <= 4 THEN 'BAG-' || s.n || '-' || k ELSE 'BAG-X-9' END, k <= 4, 'SCAN',
       s.created_at + interval '6 hours' + (k * interval '1 minute'),
       s.created_at + interval '6 hours', '0192f0a0-0000-7000-8000-00000000c101'
FROM seed_shipment s
JOIN delivery_receipt r ON r.shipment_id = s.id AND r.submitted_at IS NULL
CROSS JOIN generate_series(1, 5) AS k
WHERE s.status = 'CHECKING'
ON CONFLICT (id) DO NOTHING;

UPDATE delivery_receipt r SET submitted_at = r.received_at
FROM seed_shipment s
WHERE r.shipment_id = s.id AND s.status = 'CHECKING' AND r.submitted_at IS NULL;

INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by, created_at,
                      created_by)
SELECT ('0192f0a0-0000-7000-8000-0000001d' || lpad(n::text, 4, '0'))::uuid, id, 'DELIVERY', 252.500, 3.500, 5,
       created_at + interval '6 hours', '0192f0a0-0000-7000-8000-00000000c101', created_at + interval '6 hours',
       '0192f0a0-0000-7000-8000-00000000c101'
FROM seed_shipment
WHERE status = 'CHECKING'
ON CONFLICT (id) DO NOTHING;

INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by, recorded_at, created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000001e' || lpad(n::text, 4, '0'))::uuid, id, 'DELIVERY', 'M-SEED-' || n,
       'INTACT', '0192f0a0-0000-7000-8000-00000000c101', created_at + interval '6 hours',
       created_at + interval '6 hours', '0192f0a0-0000-7000-8000-00000000c101'
FROM seed_shipment
WHERE status = 'CHECKING'
ON CONFLICT (id) DO NOTHING;

-- Durum geçmişi: DRAFT'tan bugünkü duruma kadar her adım.
INSERT INTO shipment_status_history (id, created_at, shipment_id, from_status, to_status, changed_by, changed_at)
SELECT ('0192f0a0-0000-7000-8000-00000010' || lpad(s.n::text, 2, '0') || lpad(h.step::text, 2, '0'))::uuid,
       s.created_at + (h.step * interval '1 hour'), s.id, h.from_status, h.to_status,
       '0192f0a0-0000-7000-8000-00000000d001', s.created_at + (h.step * interval '1 hour')
FROM seed_shipment s
JOIN (VALUES (0, NULL, 'DRAFT'), (1, 'DRAFT', 'LOADED'), (2, 'LOADED', 'IN_TRANSIT'),
             (3, 'IN_TRANSIT', 'AWAITING_DELIVERY'), (4, 'AWAITING_DELIVERY', 'CHECKING')) AS h(step, from_status, to_status)
  ON h.step <= CASE s.status WHEN 'DRAFT' THEN 0 WHEN 'LOADED' THEN 1 WHEN 'IN_TRANSIT' THEN 2
                             WHEN 'AWAITING_DELIVERY' THEN 3 ELSE 4 END
ON CONFLICT (id, created_at) DO NOTHING;

-- Ana veri: sevkiyat örneklerinin başvurduğu kayıtlar ve seçim kutularında liste dolsun diye birkaç ek kayıt.
INSERT INTO md_site (id, name, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000001', 'Merkez Fabrika', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000002', 'Çorlu Depo', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;

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

INSERT INTO md_quality_grade (id, code, name, label_color, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000211', 'A', 'Birinci kalite', '#2E7D32', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000212', 'B', 'İkinci kalite', '#F9A825', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
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
