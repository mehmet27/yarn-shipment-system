-- shipment_db kısıt testleri; uygulama rolüyle çalışır ve geri alınır.
BEGIN;
-- Beklenen hata kodu üretmeyen ifade testi düşürür.
CREATE FUNCTION pg_temp.expect_error(statement text, expected_state text) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    actual_state text;
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        actual_state := SQLSTATE;
    END;
    IF actual_state IS DISTINCT FROM expected_state THEN
        RAISE EXCEPTION 'Expected SQLSTATE % but got %: %', expected_state, coalesce(actual_state, 'success'), statement;
    END IF;
END;
$$;
GRANT EXECUTE ON FUNCTION pg_temp.expect_error(text, text) TO PUBLIC;
SET ROLE shipment_app;
-- Ertelenmiş kısıtlar her ifadede denetlensin.
SET CONSTRAINTS ALL IMMEDIATE;

INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000101', gen_random_uuid(), 'S-1', gen_random_uuid(), 'Fasoncu A', 'DRAFT',
        now(), gen_random_uuid());

-- İzinsiz durum geçişi reddedilir.
SELECT pg_temp.expect_error($$UPDATE shipment SET status = 'COMPLETED'
    WHERE id = '00000000-0000-7000-8000-000000000101'$$, 'YS003');
UPDATE shipment SET status = 'LOADED' WHERE id = '00000000-0000-7000-8000-000000000101';

INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by, created_at,
                      created_by)
VALUES ('00000000-0000-7000-8000-000000000201', '00000000-0000-7000-8000-000000000101', 'DISPATCH', 2010.500, 28.000,
        40, now(), gen_random_uuid(), now(), gen_random_uuid());
DO $$ BEGIN
    IF (SELECT net_kg FROM weighing WHERE id = '00000000-0000-7000-8000-000000000201') <> 1982.500 THEN
        RAISE EXCEPTION 'net_kg must be gross - tare';
    END IF;
END $$;
-- Dara bruttan büyük olamaz.
SELECT pg_temp.expect_error($$INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count,
    weighed_at, entered_by, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000101', 'DELIVERY', 10, 20, 1, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, '23514');
-- Tartım güncellenmez; aynı taraf için ikinci geçerli tartım eklenemez.
SELECT pg_temp.expect_error($$UPDATE weighing SET gross_kg = 2100
    WHERE id = '00000000-0000-7000-8000-000000000201'$$, 'YS002');
SELECT pg_temp.expect_error($$INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count,
    weighed_at, entered_by, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000101', 'DISPATCH', 2000, 28, 40, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, '23P01');
-- Mühür numarası tek kullanımlıktır.
INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by, recorded_at, created_at, created_by)
VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000101', 'DISPATCH', 'M-1', 'INTACT', gen_random_uuid(),
        now(), now(), gen_random_uuid());
INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000102', gen_random_uuid(), 'S-2', gen_random_uuid(), 'Fasoncu B', 'DRAFT',
        now(), gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by,
    recorded_at, created_at, created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000102',
    'DISPATCH', 'M-1', 'INTACT', gen_random_uuid(), now(), now(), gen_random_uuid())$$, '23P01');

-- Kör teslim girişi gönderildikten sonra okutma eklenemez.
INSERT INTO delivery_receipt (id, shipment_id, received_by, received_at, submitted_at, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000301', '00000000-0000-7000-8000-000000000101', gen_random_uuid(), now(),
        now(), now(), gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO delivery_scan (id, delivery_receipt_id, scanned_key, matched,
    scan_method, scanned_at, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000301', 'K-1', true, 'SCAN', now(), now(), gen_random_uuid())$$, 'YS003');
SELECT pg_temp.expect_error($$UPDATE delivery_receipt SET doc_net_kg = 1
    WHERE id = '00000000-0000-7000-8000-000000000301'$$, 'YS003');

-- Sevkiyatı kilitle: kilitli sevkiyata bağlı satır eklenemez.
UPDATE shipment SET status = 'IN_TRANSIT' WHERE id = '00000000-0000-7000-8000-000000000101';
UPDATE shipment SET status = 'AWAITING_DELIVERY' WHERE id = '00000000-0000-7000-8000-000000000101';
UPDATE shipment SET status = 'CHECKING' WHERE id = '00000000-0000-7000-8000-000000000101';
UPDATE shipment SET status = 'COMPLETED', locked_at = now() WHERE id = '00000000-0000-7000-8000-000000000101';
SELECT pg_temp.expect_error($$UPDATE shipment SET e_waybill_no = 'X'
    WHERE id = '00000000-0000-7000-8000-000000000101'$$, 'YS003');
SELECT pg_temp.expect_error($$INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by,
    recorded_at, created_at, created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000101',
    'DELIVERY', 'M-1', 'INTACT', gen_random_uuid(), now(), now(), gen_random_uuid())$$, 'YS003');

-- Silme her durumda reddedilir.
SELECT pg_temp.expect_error($$DELETE FROM weighing$$, '42501');

-- Durum geçmişi yalnızca eklenir.
INSERT INTO shipment_status_history (id, created_at, shipment_id, from_status, to_status, changed_by, changed_at)
VALUES (gen_random_uuid(), now(), '00000000-0000-7000-8000-000000000101', 'DRAFT', 'LOADED', gen_random_uuid(), now());
SELECT pg_temp.expect_error($$UPDATE shipment_status_history SET reason = 'x'$$, '42501');

-- Outbox: yalnızca published_at doldurulabilir; olay tipi CloudEvents biçimindedir.
INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, occurred_at)
VALUES (gen_random_uuid(), now(), 'Shipment', '00000000-0000-7000-8000-000000000101',
        'com.yarnshipment.shipment.shipment-completed.v1', '{}', now());
UPDATE outbox_event SET published_at = now();
SELECT pg_temp.expect_error($$UPDATE outbox_event SET payload = '{"x": 1}'$$, '42501');
SELECT pg_temp.expect_error($$INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type,
    payload, occurred_at) VALUES (gen_random_uuid(), now(), 'Shipment', gen_random_uuid(), 'ShipmentCompleted',
    '{}', now())$$, '23514');
ROLLBACK;


-- Admin onaylı düzeltme (DOM-16): yeni tartım eklenir, eskisi aynı transaction'da superseded_by ile işaretlenir.
BEGIN;
-- Beklenen hata kodu üretmeyen ifade testi düşürür.
CREATE FUNCTION pg_temp.expect_error(statement text, expected_state text) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    actual_state text;
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        actual_state := SQLSTATE;
    END;
    IF actual_state IS DISTINCT FROM expected_state THEN
        RAISE EXCEPTION 'Expected SQLSTATE % but got %: %', expected_state, coalesce(actual_state, 'success'), statement;
    END IF;
END;
$$;
GRANT EXECUTE ON FUNCTION pg_temp.expect_error(text, text) TO PUBLIC;
SET ROLE shipment_app;
INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000103', gen_random_uuid(), 'S-3', gen_random_uuid(), 'Fasoncu C', 'CHECKING',
        now(), gen_random_uuid());
INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by, created_at,
                      created_by)
VALUES ('00000000-0000-7000-8000-000000000203', '00000000-0000-7000-8000-000000000103', 'DISPATCH', 100, 10, 2, now(),
        gen_random_uuid(), now(), gen_random_uuid());
UPDATE shipment SET status = 'COMPLETED', locked_at = now() WHERE id = '00000000-0000-7000-8000-000000000103';

-- Düzeltme bağlamı olmadan kilitli sevkiyata tartım eklenemez ve işaretleme yapılamaz.
SELECT pg_temp.expect_error($$INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count,
    weighed_at, entered_by, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000103', 'DISPATCH', 101, 10, 2, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, 'YS003');

SET LOCAL app.correction_record_id = '00000000-0000-7000-8000-000000000401';
INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by, created_at,
                      created_by)
VALUES ('00000000-0000-7000-8000-000000000205', '00000000-0000-7000-8000-000000000103', 'DISPATCH', 101, 10, 2,
        now(), gen_random_uuid(), now(), gen_random_uuid());
UPDATE weighing SET superseded_by = '00000000-0000-7000-8000-000000000205'
WHERE id = '00000000-0000-7000-8000-000000000203';
-- Commit anındaki denetimi şimdi çalıştır: tek geçerli tartım kaldı.
SET CONSTRAINTS ALL IMMEDIATE;
-- İşaretlenmiş satır bir daha değişmez (tekillik kısıtı yeniden ertelenir ki trigger denetlensin).
SET CONSTRAINTS ALL DEFERRED;
SELECT pg_temp.expect_error($$UPDATE weighing SET superseded_by = NULL
    WHERE id = '00000000-0000-7000-8000-000000000203'$$, 'YS002');
ROLLBACK;
