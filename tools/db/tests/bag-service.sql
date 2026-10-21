-- bag_db kısıt testleri; uygulama rolüyle çalışır ve geri alınır.
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

SET ROLE bag_app;

-- Kalitesi kilitlenmemiş partiye çuval açılamaz.
SELECT pg_temp.expect_error($$INSERT INTO bag (id, site_id, unique_key, package_type, lot_id, lot_no, product_id,
    quality_grade_code, status, created_at, created_by) VALUES ('00000000-0000-7000-8000-0000000000b1',
    gen_random_uuid(), 'K-1', 'BAG', '00000000-0000-7000-8000-0000000000a1', 'L-1', gen_random_uuid(), 'A',
    'CREATED', now(), gen_random_uuid())$$, '23503');

INSERT INTO lot_quality_lock (lot_id, site_id, lot_no, product_id, quality_grade_code, locked_at, created_at)
VALUES ('00000000-0000-7000-8000-0000000000a1', gen_random_uuid(), 'L-1', gen_random_uuid(), 'A', now(), now());

-- Kalite partiden farklı girilemez.
SELECT pg_temp.expect_error($$INSERT INTO bag (id, site_id, unique_key, package_type, lot_id, lot_no, product_id,
    quality_grade_code, status, created_at, created_by) VALUES (gen_random_uuid(), gen_random_uuid(), 'K-2', 'BAG',
    '00000000-0000-7000-8000-0000000000a1', 'L-1', gen_random_uuid(), 'B', 'CREATED', now(),
    gen_random_uuid())$$, '23503');

INSERT INTO bag (id, site_id, unique_key, package_type, lot_id, lot_no, product_id, quality_grade_code, status,
                 created_at, created_by)
VALUES ('00000000-0000-7000-8000-0000000000b1', gen_random_uuid(), 'K-1', 'BAG',
        '00000000-0000-7000-8000-0000000000a1', 'L-1', gen_random_uuid(), 'A', 'CREATED', now(), gen_random_uuid());

-- Unique Key tekildir.
SELECT pg_temp.expect_error($$INSERT INTO bag (id, site_id, unique_key, package_type, lot_id, lot_no, product_id,
    quality_grade_code, status, created_at, created_by) VALUES (gen_random_uuid(), gen_random_uuid(), 'K-1', 'BAG',
    '00000000-0000-7000-8000-0000000000a1', 'L-1', gen_random_uuid(), 'A', 'CREATED', now(),
    gen_random_uuid())$$, '23505');
-- Çuvalın partisi sonradan değişemez.
SELECT pg_temp.expect_error($$UPDATE bag SET lot_no = 'L-2' WHERE unique_key = 'K-1'$$, 'YS002');
UPDATE bag SET status = 'RESERVED' WHERE unique_key = 'K-1';

INSERT INTO bag_reservation (id, bag_id, shipment_id, reserved_at, reserved_by, created_at, created_by)
VALUES ('00000000-0000-7000-8000-0000000000c1', '00000000-0000-7000-8000-0000000000b1', gen_random_uuid(), now(),
        gen_random_uuid(), now(), gen_random_uuid());
-- Bir çuval aynı anda iki sevkiyatta olamaz.
SELECT pg_temp.expect_error($$INSERT INTO bag_reservation (id, bag_id, shipment_id, reserved_at, reserved_by,
    created_at, created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-0000000000b1', gen_random_uuid(),
    now(), gen_random_uuid(), now(), gen_random_uuid())$$, '23505');
-- Bırakmada gerekçe zorunlu; bırakılan rezervasyon tekrar değişmez.
SELECT pg_temp.expect_error($$UPDATE bag_reservation SET released_at = now(), released_by = gen_random_uuid()
    WHERE id = '00000000-0000-7000-8000-0000000000c1'$$, '23514');
UPDATE bag_reservation SET released_at = now(), released_by = gen_random_uuid(), release_reason = 'ShipmentCancelled'
WHERE id = '00000000-0000-7000-8000-0000000000c1';
SELECT pg_temp.expect_error($$UPDATE bag_reservation SET release_reason = 'x'
    WHERE id = '00000000-0000-7000-8000-0000000000c1'$$, 'YS002');
-- Bırakılan çuval yeni sevkiyata bağlanabilir.
INSERT INTO bag_reservation (id, bag_id, shipment_id, reserved_at, reserved_by, created_at, created_by)
VALUES (gen_random_uuid(), '00000000-0000-7000-8000-0000000000b1', gen_random_uuid(), now(), gen_random_uuid(),
        now(), gen_random_uuid());
-- Yeniden baskıda gerekçe zorunlu.
INSERT INTO label_print (id, bag_id, print_no, printed_at, printed_by, created_at, created_by)
VALUES (gen_random_uuid(), '00000000-0000-7000-8000-0000000000b1', 1, now(), gen_random_uuid(), now(),
        gen_random_uuid());
SELECT pg_temp.expect_error($$INSERT INTO label_print (id, bag_id, print_no, printed_at, printed_by, created_at,
    created_by) VALUES (gen_random_uuid(), '00000000-0000-7000-8000-0000000000b1', 2, now(), gen_random_uuid(),
    now(), gen_random_uuid())$$, '23514');
ROLLBACK;
