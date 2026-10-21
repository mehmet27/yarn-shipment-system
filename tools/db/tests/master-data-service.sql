-- masterdata_db kısıt testleri; uygulama rolüyle çalışır ve geri alınır.
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

SET ROLE masterdata_app;

INSERT INTO site (id, code, name, site_type, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000001', 'F1', 'Fabrika', 'FACTORY', 'ACTIVE', now(), gen_random_uuid());
INSERT INTO product (id, code, name, packaging_type, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000002', 'P1', 'Ne 30/1', 'BAG', 'ACTIVE', now(), gen_random_uuid());
INSERT INTO quality_grade (id, code, name, status, created_at, created_by) VALUES
    ('00000000-0000-7000-8000-000000000003', 'A', 'A kalite', 'ACTIVE', now(), gen_random_uuid()),
    ('00000000-0000-7000-8000-000000000004', 'B', 'B kalite', 'ACTIVE', now(), gen_random_uuid());
INSERT INTO production_lot (id, site_id, lot_no, product_id, quality_grade_id, production_date,
                            quality_locked_at, quality_locked_by, status, created_at, created_by)
VALUES ('00000000-0000-7000-8000-000000000005', '00000000-0000-7000-8000-000000000001', 'L-1',
        '00000000-0000-7000-8000-000000000002', '00000000-0000-7000-8000-000000000003', current_date,
        now(), gen_random_uuid(), 'ACTIVE', now(), gen_random_uuid());

-- Kilitli partinin kalitesi değişemez.
SELECT pg_temp.expect_error($$UPDATE production_lot SET quality_grade_id = '00000000-0000-7000-8000-000000000004'
    WHERE id = '00000000-0000-7000-8000-000000000005'$$, 'YS004');
-- Aynı lokasyonda parti numarası tekildir.
SELECT pg_temp.expect_error($$INSERT INTO production_lot (id, site_id, lot_no, product_id, quality_grade_id,
    production_date, status, created_at, created_by) VALUES (gen_random_uuid(),
    '00000000-0000-7000-8000-000000000001', 'L-1', '00000000-0000-7000-8000-000000000002',
    '00000000-0000-7000-8000-000000000003', current_date, 'ACTIVE', now(), gen_random_uuid())$$, '23505');
-- Fasoncu kullanıcısı fasoncu bağı olmadan açılamaz.
SELECT pg_temp.expect_error($$INSERT INTO user_profile (id, keycloak_user_id, full_name, user_type, status,
    created_at, created_by) VALUES (gen_random_uuid(), gen_random_uuid(), 'X', 'SUBCONTRACTOR', 'ACTIVE', now(),
    gen_random_uuid())$$, '23514');
-- Uygulama rolü silemez (GEN-04).
SELECT pg_temp.expect_error($$DELETE FROM site$$, '42501');
-- Kritik tablo değişikliği yerel ize düşer; uygulama rolü bu izi okuyamaz.
SELECT pg_temp.expect_error($$SELECT * FROM audit_local$$, '42501');
RESET ROLE;
DO $$ BEGIN
    IF (SELECT count(*) FROM audit_local WHERE table_name = 'production_lot') <> 1 THEN
        RAISE EXCEPTION 'audit_local did not capture production_lot insert';
    END IF;
END $$;
ROLLBACK;
