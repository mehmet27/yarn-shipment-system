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

-- Outbox tablosu kaldırıldı (olaylar süreç içi taşınır); migration'ın tabloyu düşürdüğü doğrulanır.
DO $$ BEGIN
    IF to_regclass('outbox_event') IS NOT NULL THEN RAISE EXCEPTION 'outbox_event hala var'; END IF;
END $$;
ROLLBACK;


-- Kantar uyuşmazlığı (süreç analizi 6.2): WEIGHT_MISMATCH geçişleri, reçete satırları, fire oranı ve weight_check.
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
SET CONSTRAINTS ALL IMMEDIATE;

INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, planned_net_kg,
                      created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000111', gen_random_uuid(), 'S-W1', gen_random_uuid(), 'Fasoncu W', 'DRAFT',
        1982.500, now(), gen_random_uuid());
DO $$ BEGIN
    IF (SELECT fire_rate FROM shipment WHERE id = '00000000-0000-7000-8000-000000000111') <> 0.04 THEN
        RAISE EXCEPTION 'default fire_rate must be 0.04';
    END IF;
END $$;
-- Fire oranı 0..0.20 arasında olmalı.
SELECT pg_temp.expect_error($$UPDATE shipment SET fire_rate = 0.21
    WHERE id = '00000000-0000-7000-8000-000000000111'$$, '23514');
SELECT pg_temp.expect_error($$UPDATE shipment SET fire_rate = -0.01
    WHERE id = '00000000-0000-7000-8000-000000000111'$$, '23514');

-- Reçete satırı: net kg pozitif, cins zorunlu, satır no tekil.
INSERT INTO shipment_line (id, shipment_id, line_no, yarn_type_text, net_kg, created_at, created_by)
VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000111', 1, '36/1 VİSKON', 1982.500, now(),
        gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO shipment_line (id, shipment_id, line_no, yarn_type_text, net_kg,
    created_at, created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000111', 2, 'x', 0, now(),
    gen_random_uuid())$$, '23514');
SELECT pg_temp.expect_error($$INSERT INTO shipment_line (id, shipment_id, line_no, net_kg, created_at, created_by)
    VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000111', 2, 10, now(), gen_random_uuid())$$, '23514');
SELECT pg_temp.expect_error($$INSERT INTO shipment_line (id, shipment_id, line_no, yarn_type_text, net_kg,
    created_at, created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-000000000111', 1, 'y', 10, now(),
    gen_random_uuid())$$, '23505');
-- Satır silinemez.
SELECT pg_temp.expect_error($$DELETE FROM shipment_line
    WHERE shipment_id = '00000000-0000-7000-8000-000000000111'$$, 'YS001');

-- DRAFT -> WEIGHT_MISMATCH -> DRAFT -> WEIGHT_MISMATCH -> AWAITING_DELIVERY; uyuşmazlıktan COMPLETED olmaz.
UPDATE shipment SET status = 'WEIGHT_MISMATCH' WHERE id = '00000000-0000-7000-8000-000000000111';
SELECT pg_temp.expect_error($$UPDATE shipment SET status = 'COMPLETED'
    WHERE id = '00000000-0000-7000-8000-000000000111'$$, 'YS003');
UPDATE shipment SET status = 'DRAFT' WHERE id = '00000000-0000-7000-8000-000000000111';
UPDATE shipment SET status = 'WEIGHT_MISMATCH' WHERE id = '00000000-0000-7000-8000-000000000111';

-- weight_check: fark ve karar tutarlılığı.
INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by, created_at,
                      created_by)
VALUES ('00000000-0000-7000-8000-000000000211', '00000000-0000-7000-8000-000000000111', 'DISPATCH', 2000, 10, 40,
        now(), gen_random_uuid(), now(), gen_random_uuid());
INSERT INTO weight_check (id, shipment_id, weighing_id, empty_kg, loaded_kg, expected_kg, diff_kg, tolerance_kg,
                          created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000311', '00000000-0000-7000-8000-000000000111',
        '00000000-0000-7000-8000-000000000211', 10, 2000, 1982.500, 7.500, 0, now(), gen_random_uuid());
-- Karar eksik alanla yazılamaz; geçersiz karar reddedilir.
SELECT pg_temp.expect_error($$UPDATE weight_check SET decision = 'UPDATE_RECIPE'
    WHERE id = '00000000-0000-7000-8000-000000000311'$$, '23514');
SELECT pg_temp.expect_error($$UPDATE weight_check SET decision = 'IGNORE', decided_by = gen_random_uuid(),
    decided_at = now(), reason = 'x' WHERE id = '00000000-0000-7000-8000-000000000311'$$, '23514');
UPDATE weight_check SET decision = 'UPDATE_RECIPE', decided_by = gen_random_uuid(), decided_at = now(),
                        reason = 'Fark kabul edildi'
WHERE id = '00000000-0000-7000-8000-000000000311';
SELECT pg_temp.expect_error($$DELETE FROM weight_check
    WHERE id = '00000000-0000-7000-8000-000000000311'$$, 'YS001');
UPDATE shipment SET status = 'AWAITING_DELIVERY' WHERE id = '00000000-0000-7000-8000-000000000111';

-- Uyuşmazlıktaki sevkiyat iptal edilebilir.
INSERT INTO shipment (id, site_id, shipment_no, subcontractor_id, subcontractor_title, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000112', gen_random_uuid(), 'S-W2', gen_random_uuid(), 'Fasoncu W2',
        'WEIGHT_MISMATCH', now(), gen_random_uuid());
UPDATE shipment SET status = 'CANCELLED', cancelled_reason = 'Yanlış yükleme'
WHERE id = '00000000-0000-7000-8000-000000000112';
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
INSERT INTO md_quality_grade (id, code, name, sort_order, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000603', 'A', 'Birinci kalite', 1, now(), gen_random_uuid()),
       ('00000000-0000-7000-8000-000000000604', 'B', 'İkinci kalite', 2, now(), gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO md_product (id, code, name, created_at, created_by)
    VALUES (gen_random_uuid(), 'pes-30', 'Tekrar', now(), gen_random_uuid())$$, '23505');
SELECT pg_temp.expect_error($$UPDATE md_quality_grade SET code = 'C'
    WHERE id = '00000000-0000-7000-8000-000000000603'$$, 'YS002');
SELECT pg_temp.expect_error($$INSERT INTO md_quality_grade (id, code, name, sort_order, created_at, created_by)
    VALUES (gen_random_uuid(), 'X', 'Sıra', -1, now(), gen_random_uuid())$$, '23514');
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

ROLLBACK;
