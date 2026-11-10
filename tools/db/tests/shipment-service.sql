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
-- Ertelenmiş kısıtlar her ifadede denetlensin.
SET CONSTRAINTS ALL IMMEDIATE;

INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000101', gen_random_uuid(), 'S-1', gen_random_uuid(), 'Fasoncu A', 'DRAFT',
        now(), gen_random_uuid());

-- İzinsiz durum geçişi reddedilir.
SELECT pg_temp.expect_error($$UPDATE shipment SET status = 'COMPLETED'
    WHERE id = '00000000-0000-7000-8000-000000000101'$$, 'YS003');
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
    '00000000-0000-7000-8000-000000000101', 'DISPATCH', 10, 20, 1, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, '23514');
-- Teslim tarafı tartımı artık yoktur.
SELECT pg_temp.expect_error($$INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count,
    weighed_at, entered_by, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000101', 'DELIVERY', 20, 10, 1, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, '23514');
-- Tartım güncellenmez; aynı taraf için ikinci geçerli tartım eklenemez.
SELECT pg_temp.expect_error($$UPDATE weighing SET gross_kg = 2100
    WHERE id = '00000000-0000-7000-8000-000000000201'$$, 'YS002');
SELECT pg_temp.expect_error($$INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count,
    weighed_at, entered_by, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000101', 'DISPATCH', 2000, 28, 40, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, '23P01');

-- Kaldırılan ara durumlar yazılamaz; taslak yalnızca fasoncuya yolda olur.
SELECT pg_temp.expect_error($$UPDATE shipment SET status = 'LOADED'
    WHERE id = '00000000-0000-7000-8000-000000000101'$$, 'YS003');
UPDATE shipment SET status = 'AWAITING_DELIVERY' WHERE id = '00000000-0000-7000-8000-000000000101';
SELECT pg_temp.expect_error($$UPDATE shipment SET status = 'DRAFT'
    WHERE id = '00000000-0000-7000-8000-000000000101'$$, 'YS003');

-- Sevkiyat irsaliyeyle tamamlanır ve kilitlenir: kilitli sevkiyat değişmez.
UPDATE shipment SET status = 'COMPLETED', e_waybill_no = 'IRS-1', waybill_evidence_id = gen_random_uuid(),
                    locked_at = now()
WHERE id = '00000000-0000-7000-8000-000000000101';
SELECT pg_temp.expect_error($$UPDATE shipment SET e_waybill_no = 'X'
    WHERE id = '00000000-0000-7000-8000-000000000101'$$, 'YS003');
SELECT pg_temp.expect_error($$INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count,
    weighed_at, entered_by, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000101', 'DISPATCH', 101, 10, 2, now(), gen_random_uuid(), now(),
    gen_random_uuid())$$, 'YS003');

-- Durum geçmişi yalnızca eklenir.
INSERT INTO shipment_status_history (id, created_at, shipment_id, from_status, to_status, changed_by, changed_at)
VALUES (gen_random_uuid(), now(), '00000000-0000-7000-8000-000000000101', 'DRAFT', 'AWAITING_DELIVERY',
        gen_random_uuid(), now());

-- Outbox: yalnızca published_at doldurulabilir; olay tipi CloudEvents biçimindedir.
INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, occurred_at)
VALUES (gen_random_uuid(), now(), 'Shipment', '00000000-0000-7000-8000-000000000101',
        'com.yarnshipment.shipment.shipment-completed.v1', '{}', now());
UPDATE outbox_event SET published_at = now();
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
INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000103', gen_random_uuid(), 'S-3', gen_random_uuid(), 'Fasoncu C', 'AWAITING_DELIVERY',
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

-- Parti kalite kilidi (rapor Bölüm 10): kilitsiz parti düzenlenir; kilitli partinin kalitesi, ürünü, numarası ve
-- kilidi değişmez, yalnızca pasife alınır. Kodlar eklendikten sonra değişmez.
INSERT INTO md_site (id, name, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000601', 'Lokasyon', now(), gen_random_uuid()),
       ('00000000-0000-7000-8000-000000000606', 'Diğer lokasyon', now(), gen_random_uuid());
INSERT INTO md_product (id, code, name, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000602', 'PES-30', 'Polyester 30/1', now(), gen_random_uuid());
INSERT INTO md_quality_grade (id, code, name, label_color, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000603', 'A', 'Birinci kalite', '#2E7D32', now(), gen_random_uuid()),
       ('00000000-0000-7000-8000-000000000604', 'B', 'İkinci kalite', NULL, now(), gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO md_product (id, code, name, created_at, created_by)
    VALUES (gen_random_uuid(), 'pes-30', 'Tekrar', now(), gen_random_uuid())$$, '23505');
SELECT pg_temp.expect_error($$UPDATE md_quality_grade SET code = 'C'
    WHERE id = '00000000-0000-7000-8000-000000000603'$$, 'YS002');
SELECT pg_temp.expect_error($$INSERT INTO md_quality_grade (id, code, name, label_color, created_at, created_by)
    VALUES (gen_random_uuid(), 'X', 'Renk', 'red', now(), gen_random_uuid())$$, '23514');
INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id, production_date, created_at,
                               created_by)
VALUES ('00000000-0000-7000-8000-000000000605', '00000000-0000-7000-8000-000000000601', 'P-1',
        '00000000-0000-7000-8000-000000000602', '00000000-0000-7000-8000-000000000603', current_date, now(),
        gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO md_production_lot (id, site_id, lot_no, product_id, quality_grade_id,
    production_date, created_at, created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000601',
    'p-1', '00000000-0000-7000-8000-000000000602', '00000000-0000-7000-8000-000000000603', current_date, now(),
    gen_random_uuid())$$, '23505');
UPDATE md_production_lot SET quality_grade_id = '00000000-0000-7000-8000-000000000604'
WHERE id = '00000000-0000-7000-8000-000000000605';
SELECT pg_temp.expect_error($$UPDATE md_production_lot SET quality_locked_at = now()
    WHERE id = '00000000-0000-7000-8000-000000000605'$$, '23514');
UPDATE md_production_lot SET quality_locked_at = now(), quality_locked_by = gen_random_uuid()
WHERE id = '00000000-0000-7000-8000-000000000605';
SELECT pg_temp.expect_error($$UPDATE md_production_lot SET quality_grade_id = '00000000-0000-7000-8000-000000000603'
    WHERE id = '00000000-0000-7000-8000-000000000605'$$, 'YS004');
SELECT pg_temp.expect_error($$UPDATE md_production_lot SET quality_locked_at = NULL, quality_locked_by = NULL
    WHERE id = '00000000-0000-7000-8000-000000000605'$$, 'YS004');
SELECT pg_temp.expect_error($$UPDATE md_production_lot SET site_id = '00000000-0000-7000-8000-000000000606'
    WHERE id = '00000000-0000-7000-8000-000000000605'$$, 'YS002');
UPDATE md_production_lot SET active = false WHERE id = '00000000-0000-7000-8000-000000000605';

-- Şifre sıfırlama kodu: kullanıcı başına tek satır, süre sonradan başlamalı, deneme sayacı eksiye düşmez.
INSERT INTO md_password_reset_code (user_id, code_hash, salt, issued_at, expires_at)
VALUES ('00000000-0000-7000-8000-000000000701', 'h', 's', now(), now() + interval '10 minutes');
SELECT pg_temp.expect_error($$INSERT INTO md_password_reset_code (user_id, code_hash, salt, issued_at, expires_at)
    VALUES ('00000000-0000-7000-8000-000000000701', 'h', 's', now(), now() + interval '10 minutes')$$, '23505');
SELECT pg_temp.expect_error($$INSERT INTO md_password_reset_code (user_id, code_hash, salt, issued_at, expires_at)
    VALUES ('00000000-0000-7000-8000-000000000702', 'h', 's', now(), now() - interval '1 minute')$$, '23514');
SELECT pg_temp.expect_error($$UPDATE md_password_reset_code SET attempts = -1
    WHERE user_id = '00000000-0000-7000-8000-000000000701'$$, '23514');
UPDATE md_password_reset_code SET attempts = attempts + 1 WHERE user_id = '00000000-0000-7000-8000-000000000701';
DELETE FROM md_password_reset_code WHERE user_id = '00000000-0000-7000-8000-000000000701';
ROLLBACK;
