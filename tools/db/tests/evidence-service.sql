-- evidence_db kısıt testleri; uygulama rolüyle çalışır ve geri alınır.
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
SET ROLE evidence_app;

INSERT INTO evidence_file (id, created_at, shipment_id, evidence_type, storage_key, mime_type, size_bytes, sha256,
                           source, uploaded_at, uploaded_by, status, created_by)
VALUES ('00000000-0000-7000-8000-000000000501', now(), gen_random_uuid(), 'DISPATCH_SCALE', 'evidence/k1',
        'image/jpeg', 1024, repeat('a', 64), 'CAMERA', now(), gen_random_uuid(), 'VALID', gen_random_uuid());
-- Konum yalnızca açık rızayla yazılır.
SELECT pg_temp.expect_error($$INSERT INTO evidence_file (id, created_at, shipment_id, evidence_type, storage_key,
    mime_type, size_bytes, sha256, source, uploaded_at, uploaded_by, status, created_by, geo_lat, geo_lon)
    VALUES (gen_random_uuid(), now(), gen_random_uuid(), 'SEAL', 'evidence/k2', 'image/jpeg', 1, repeat('b', 64),
    'CAMERA', now(), gen_random_uuid(), 'VALID', gen_random_uuid(), 41.0, 29.0)$$, '23514');
-- Hash ve dosya anahtarı değişemez; kayıt silinemez.
SELECT pg_temp.expect_error($$UPDATE evidence_file SET sha256 = repeat('c', 64)$$, 'YS002');
SELECT pg_temp.expect_error($$DELETE FROM evidence_file$$, '42501');
-- Geçersiz sayma gerekçe ister ve geri alınamaz.
SELECT pg_temp.expect_error($$UPDATE evidence_file SET status = 'INVALIDATED'$$, '23514');
UPDATE evidence_file SET status = 'INVALIDATED', invalidated_reason = 'Yanlış sevkiyata yüklendi';
SELECT pg_temp.expect_error($$UPDATE evidence_file SET status = 'VALID', invalidated_reason = NULL$$, 'YS002');
ROLLBACK;
