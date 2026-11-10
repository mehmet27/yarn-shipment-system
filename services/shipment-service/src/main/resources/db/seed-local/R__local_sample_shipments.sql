-- Yalnızca yerel geliştirme (application-local.yml): ekranlar veri göstersin diye farklı durum ve lokasyonlarda
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

-- Çıkış tartımı: DRAFT dışındaki her sevkiyat. Brüt 253.500, dara 3.500, net 250.000, beş çuval.
INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, scale_ticket_no, weighed_at, entered_by,
                      created_at, created_by)
SELECT ('0192f0a0-0000-7000-8000-0000000d' || lpad(n::text, 4, '0'))::uuid, id, 'DISPATCH', 253.500, 3.500, 5,
       'KF-' || n, created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001',
       created_at + interval '2 hours', '0192f0a0-0000-7000-8000-00000000d001'
FROM seed_shipment
WHERE status <> 'DRAFT'
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

-- Outbox: geçmişteki her adım için gerçek akıştaki olay (EVT-03). Örnekler yerel veridir; tüketicilere tekrar
-- gönderilmesin diye yayımlanmış sayılır (published_at dolu).
INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers, occurred_at,
                          published_at)
SELECT ('0192f0a0-0000-7000-8000-00000020' || lpad(s.n::text, 2, '0') || lpad(h.step::text, 2, '0'))::uuid,
       s.created_at + (h.step * interval '1 hour'), 'Shipment', s.id, h.event_type,
       CASE h.step
           WHEN 0 THEN jsonb_build_object('shipmentId', s.id, 'shipmentNo', s.shipment_no, 'siteId', s.site_id,
                                          'subcontractorId', s.subcontractor_id, 'createdAt', s.created_at)
           WHEN 1 THEN jsonb_build_object('shipmentId', s.id, 'siteId', s.site_id,
                                          'subcontractorId', s.subcontractor_id, 'bagCount', s.planned_bags,
                                          'netKg', s.planned_kg, 'dispatchedAt', s.created_at + interval '2 hours')
           ELSE jsonb_build_object('shipmentId', s.id, 'siteId', s.site_id,
                                   'completedAt', s.created_at + interval '2 days')
       END,
       jsonb_build_object('actorId', '0192f0a0-0000-7000-8000-00000000d001'),
       s.created_at + (h.step * interval '1 hour'), s.created_at + (h.step * interval '1 hour')
FROM seed_shipment s
JOIN (VALUES (0, 'com.yarnshipment.shipment.shipment-created.v1'),
             (1, 'com.yarnshipment.shipment.shipment-dispatched.v2'),
             (2, 'com.yarnshipment.shipment.shipment-completed.v1')) AS h(step, event_type)
  ON h.step <= CASE s.status WHEN 'DRAFT' THEN 0 WHEN 'AWAITING_DELIVERY' THEN 1 ELSE 2 END
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

-- İplik tedarikçileri (süreç analizi v2, Bölüm 5.1).
INSERT INTO md_yarn_supplier (id, name, created_at, created_by)
VALUES ('0192f0a0-0000-7000-8000-000000000301', 'Şireci', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000302', 'Karesi', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000303', 'Teksmar', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000304', 'Migiteks', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001'),
       ('0192f0a0-0000-7000-8000-000000000305', 'Creora', '2026-10-01T08:00:00Z', '0192f0a0-0000-7000-8000-00000000d001')
ON CONFLICT (id) DO NOTHING;
